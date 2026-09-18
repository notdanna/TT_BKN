"""
entrenamiento_efficientnetb3.py

Entrena un clasificador binario de corteza (sano / infestado) con
transfer learning en dos fases sobre EfficientNet-B3, pensado para un
dataset MODERADAMENTE DESBALANCEADO, LIMITADO EN VOLUMEN y AUN NO
COMPLETAMENTE DEPURADO.
Cada decision importante esta comentada con el por que, no solo el que.

1. EarlyStopping/ModelCheckpoint ya NO monitorean solo val_recall.
   val_recall en un problema binario con sigmoid mide unicamente el
   recall de la clase 1 (infestado) e ignora por completo cuantos
   sanos se estan clasificando mal. Un modelo que dice "infestado"
   casi siempre tiene val_recall altisimo y aun asi es un mal modelo.
   Ahora se monitorea val_fbeta_infestado, calculado por un callback
   propio que SI toma en cuenta la clase sano.

2. Los pesos de clase ya no aplican el balanceo completo de sklearn
   por default -- se atenuan con --suavizado_pesos, porque el
   desbalance real (3,452 sano vs 2,634 infestado, ~1.3:1) es leve y
   el balanceo completo ya empuja de mas hacia infestado.

3. El umbral de decision final ya NO se asume en 0.5. Se busca
   explicitamente en el set de VALIDACION con --beta, favoreciendo
   precision sobre recall (beta < 1) porque el problema reportado es
   demasiados falsos "infestado", no al reves.

Instalar dependencias:
    pip install -r requirements.txt

Uso tipico:
    python entrenamiento_efficientnetb3.py \
        --data_dir ./dataset \
        --manifest manifest.csv \
        --epocas_fase1 10 \
        --epocas_fase2 30
"""

import argparse
import random
import re
from pathlib import Path
from typing import Optional

import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import GroupShuffleSplit
from sklearn.utils.class_weight import compute_class_weight
from sklearn.metrics import classification_report, confusion_matrix, precision_recall_curve

# ----------------------------------------------------------------------
# 0. REPRODUCIBILIDAD Y CONFIGURACION DE GPU
# ----------------------------------------------------------------------
SEED = 42
random.seed(SEED)
np.random.seed(SEED)
tf.random.set_seed(SEED)

gpus = tf.config.list_physical_devices("GPU")
print(f"GPUs detectadas por TensorFlow: {len(gpus)}")
if gpus:
    try:
        for _gpu in gpus:
            tf.config.experimental.set_memory_growth(_gpu, True)
    except RuntimeError as e:
        # En Apple Silicon (Metal) es normal que esto falle de forma segura
        print(f"Gestión de memoria nativa de Apple Silicon activada.")

IMG_SIZE = 300
BATCH_SIZE = 16
AUTOTUNE = tf.data.AUTOTUNE
CLASE_A_INDICE = {"sano": 0, "infestado": 1}

GROUP_REGEX = re.compile(r"^([A-Za-z]*\d+)")

# ----------------------------------------------------------------------
# 1. CONSTRUCCION DEL DATAFRAME DE TRABAJO
# ----------------------------------------------------------------------
def inferir_grupo(filepath: str) -> str:
    nombre = Path(filepath).stem
    m = GROUP_REGEX.match(nombre)
    return m.group(1) if m else nombre

def construir_dataframe(data_dir: Path, manifest_path: Optional[Path]) -> pd.DataFrame:
    if manifest_path and manifest_path.exists():
        df = pd.read_csv(manifest_path)
        antes = len(df)
        df = df[df["valida"] == True]  # noqa: E712
        df = df.drop_duplicates(subset="sha256")
        if "phash_dup" in df.columns:
            df = df[~df["phash_dup"].fillna(False)]
        print(f"Manifest cargado: {antes} filas -> {len(df)} tras filtrar corruptas/duplicadas.")
        df = df[["filepath", "clase"]].copy()
    else:
        print("No se encontro manifest.csv; leyendo las carpetas directamente.")
        filas = []
        carpetas = {"sano": "sano", "infestado": "infestado"}
        for clase, nombre_carpeta in carpetas.items():
            for p in (data_dir / nombre_carpeta).rglob("*"):
                if p.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp", ".bmp"}:
                    filas.append({"filepath": str(p), "clase": clase})
        df = pd.DataFrame(filas)

    df["grupo"] = df["filepath"].apply(inferir_grupo)

    # AVISO: si tus imagenes pasaron por el script de copiado/renombrado
    # por confianza (mv.py) antes de llegar a esta carpeta, es muy
    # probable que el nombre de archivo ya NO conserve el identificador
    # de arbol original (p.ej. si ahora se llaman "0.930_infestado_001.jpg").
    # En ese caso GROUP_REGEX infiere grupos sin sentido y el split
    # 80/10/10 deja de ser "por arbol" -- se reintroduce la fuga de datos
    # que ya habiamos identificado como riesgo. Revisa unos nombres reales
    # de dataset_limpio/sano y dataset_limpio/infestado antes de confiar
    # en este split.
    n_grupos = df["grupo"].nunique()
    print(f"Grupos (arboles) inferidos de los nombres de archivo: {n_grupos}")
    if n_grupos >= len(df) * 0.9:
        print(
            "ADVERTENCIA: casi cada imagen quedo en su propio grupo. Es una señal "
            "de que inferir_grupo() NO esta detectando el arbol real -- revisa el "
            "regex o los nombres de archivo antes de entrenar, o el split dejara "
            "de proteger contra fuga de datos entre train/val/test."
        )

    return df

# ----------------------------------------------------------------------
# 2. DIVISION 80/10/10 CONSCIENTE DE GRUPOS
# ----------------------------------------------------------------------
def dividir_dataset(df: pd.DataFrame):
    gss1 = GroupShuffleSplit(n_splits=1, test_size=0.20, random_state=SEED)
    idx_train, idx_temp = next(gss1.split(df, groups=df["grupo"]))
    df_train, df_temp = df.iloc[idx_train].copy(), df.iloc[idx_temp].copy()

    gss2 = GroupShuffleSplit(n_splits=1, test_size=0.50, random_state=SEED)
    idx_val, idx_test = next(gss2.split(df_temp, groups=df_temp["grupo"]))
    df_val, df_test = df_temp.iloc[idx_val].copy(), df_temp.iloc[idx_test].copy()

    for nombre, sub in [("train", df_train), ("val", df_val), ("test", df_test)]:
        print(f"{nombre}: {len(sub)} imagenes | {sub['clase'].value_counts().to_dict()}")

    return df_train, df_val, df_test

# ----------------------------------------------------------------------
# 3. PIPELINE tf.data
# ----------------------------------------------------------------------
def cargar_imagen(filepath, label):
    img = tf.io.read_file(filepath)
    img = tf.image.decode_image(img, channels=3, expand_animations=False)
    img = tf.image.resize(img, [IMG_SIZE, IMG_SIZE])
    img.set_shape([IMG_SIZE, IMG_SIZE, 3])
    return img, label

def construir_tfdataset(df: pd.DataFrame, entrenamiento: bool) -> tf.data.Dataset:
    labels = df["clase"].map(CLASE_A_INDICE).values.astype("int32")
    ds = tf.data.Dataset.from_tensor_slices((df["filepath"].values, labels))
    if entrenamiento:
        ds = ds.shuffle(buffer_size=len(df), seed=SEED, reshuffle_each_iteration=True)
    ds = ds.map(cargar_imagen, num_parallel_calls=AUTOTUNE)
    if not entrenamiento:
        # val/test ahora se recorren varias veces (metricas por epoca +
        # busqueda de umbral), cachear evita releer/redecodificar cada vez
        ds = ds.cache()
    ds = ds.batch(BATCH_SIZE).prefetch(AUTOTUNE)
    return ds

# ----------------------------------------------------------------------
# 3.5 CALLBACK: METRICAS DE VALIDACION QUE SI TOMAN EN CUENTA A "SANO"
# ----------------------------------------------------------------------
class MetricasValidacionCompletas(tf.keras.callbacks.Callback):
    """
    Al final de cada epoca calcula la matriz de confusion completa sobre
    el set de validacion y agrega al log metricas que consideran ambas
    clases -- no solo el recall de "infestado" que Keras trae por default.

    Por que: EarlyStopping/ModelCheckpoint monitoreando unicamente
    'val_recall' pueden terminar quedandose con la epoca donde el modelo
    dice "infestado" casi siempre (recall altisimo, precision pesima).
    Es exactamente el sesgo reportado en las corridas anteriores.

    beta < 1 en el F-beta pondera mas la precision que el recall --
    preferimos, a proposito, un modelo mas conservador para declarar
    "infestado".
    """
    def __init__(self, ds_val: tf.data.Dataset, y_val: np.ndarray, beta: float = 0.7):
        super().__init__()
        self.ds_val = ds_val
        self.y_val = y_val
        self.beta = beta

    def on_epoch_end(self, epoch, logs=None):
        logs = logs if logs is not None else {}
        y_prob = self.model.predict(self.ds_val, verbose=0).ravel()
        y_pred = (y_prob >= 0.5).astype(int)

        tn, fp, fn, tp = confusion_matrix(self.y_val, y_pred, labels=[0, 1]).ravel()

        recall_infestado = tp / (tp + fn) if (tp + fn) > 0 else 0.0
        recall_sano = tn / (tn + fp) if (tn + fp) > 0 else 0.0  # especificidad
        precision_infestado = tp / (tp + fp) if (tp + fp) > 0 else 0.0

        balanced_acc = (recall_infestado + recall_sano) / 2
        if (precision_infestado + recall_infestado) > 0:
            f_beta = (
                (1 + self.beta**2) * precision_infestado * recall_infestado
                / (self.beta**2 * precision_infestado + recall_infestado)
            )
        else:
            f_beta = 0.0

        logs["val_recall_sano"] = recall_sano
        logs["val_balanced_accuracy"] = balanced_acc
        logs["val_fbeta_infestado"] = f_beta

        print(
            f"  val_recall_sano(especificidad)={recall_sano:.3f}  "
            f"val_balanced_accuracy={balanced_acc:.3f}  "
            f"val_fbeta_infestado(beta={self.beta})={f_beta:.3f}  "
            f"[FP={fp} sanos marcados infestado, FN={fn} infestados no detectados]"
        )

# ----------------------------------------------------------------------
# 4. MODELO: AUMENTO DE DATOS + BASE EFFICIENTNET-B3 + HEAD
# ----------------------------------------------------------------------
def construir_modelo(fine_tune_from: Optional[int] = None, dropout: float = 0.3) -> tf.keras.Model:
    aumento_de_datos = tf.keras.Sequential(
        [
            tf.keras.layers.RandomFlip("horizontal_and_vertical"),
            tf.keras.layers.RandomRotation(0.25),
            tf.keras.layers.RandomBrightness(0.15, value_range=(0, 255)),
            tf.keras.layers.RandomContrast(0.15),
            tf.keras.layers.RandomZoom(0.1),
        ],
        name="aumento_de_datos",
    )

    base_model = tf.keras.applications.EfficientNetB3(
        include_top=False,
        weights="imagenet",
        input_shape=(IMG_SIZE, IMG_SIZE, 3),
        pooling="avg",
    )

    if fine_tune_from is None:
        base_model.trainable = False
    else:
        base_model.trainable = True
        for layer in base_model.layers[:fine_tune_from]:
            layer.trainable = False

    for layer in base_model.layers:
        if isinstance(layer, tf.keras.layers.BatchNormalization):
            layer.trainable = False

    inputs = tf.keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3))
    x = aumento_de_datos(inputs)
    x = base_model(x, training=True)
    x = tf.keras.layers.Dropout(dropout, name="dropout_regularizacion")(x)
    # L2 pequeño en la capa final: con un dataset todavia limitado (~6k
    # imagenes en total) es un seguro barato contra sobreajuste
    outputs = tf.keras.layers.Dense(
        1,
        activation="sigmoid",
        name="salida_infestado",
        kernel_regularizer=tf.keras.regularizers.l2(1e-4),
    )(x)

    return tf.keras.Model(inputs, outputs, name="clasificador_corteza_efficientnetb3")

# ----------------------------------------------------------------------
# 5. PESOS DE CLASE
# ----------------------------------------------------------------------
def calcular_class_weights(df_train: pd.DataFrame, suavizado: float = 0.6) -> dict:
    """
    suavizado=1.0 -> balanceo completo (formula 'balanced' de sklearn)
    suavizado=0.0 -> sin ponderar (ambas clases pesan 1.0)

    Por que atenuar: con el dataset actual (~3,452 sanas vs ~2,634
    infestadas, razon ~1.3:1) el desbalance es leve. El peso "balanced"
    completo ya empuja al modelo a priorizar infestado -- justo la
    direccion del sesgo reportado. Un suavizado entre 0.4 y 0.7 conserva
    algo de compensacion sin exagerarla.
    """
    y = df_train["clase"].map(CLASE_A_INDICE).values
    pesos_balanced = compute_class_weight(class_weight="balanced", classes=np.array([0, 1]), y=y)
    pesos_finales = 1.0 + suavizado * (pesos_balanced - 1.0)
    class_weight = {0: float(pesos_finales[0]), 1: float(pesos_finales[1])}
    print(f"Pesos de clase (suavizado={suavizado}): {class_weight}")
    return class_weight

# ----------------------------------------------------------------------
# 6. COMPILACION Y CALLBACKS
# ----------------------------------------------------------------------
def compilar(modelo: tf.keras.Model, learning_rate: float):
    modelo.compile(
        # CORRECCIÓN: Uso de legacy.Adam para evitar el error de Mutation en M1/M2
        optimizer=tf.keras.optimizers.legacy.Adam(learning_rate=learning_rate),
        loss=tf.keras.losses.BinaryCrossentropy(label_smoothing=0.1),
        metrics=[
            tf.keras.metrics.BinaryAccuracy(name="accuracy"),
            tf.keras.metrics.AUC(name="auc"),
            tf.keras.metrics.Precision(name="precision"),
            tf.keras.metrics.Recall(name="recall"),
        ],
    )

def callbacks_para(
    nombre_checkpoint: str,
    ds_val: tf.data.Dataset,
    y_val: np.ndarray,
    beta: float = 0.7,
    monitor: str = "val_fbeta_infestado",
):
    # metricas_cb va PRIMERO en la lista a proposito: Keras pasa el mismo
    # dict "logs" a cada callback en orden dentro de la misma epoca, asi
    # que si este callback agrega val_fbeta_infestado antes, EarlyStopping
    # y ModelCheckpoint ya lo pueden leer en esa misma epoca.
    metricas_cb = MetricasValidacionCompletas(ds_val, y_val, beta=beta)
    return [
        metricas_cb,
        tf.keras.callbacks.EarlyStopping(
            monitor=monitor, mode="max", patience=6, restore_best_weights=True
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss", factor=0.5, patience=3, min_lr=1e-7
        ),
        tf.keras.callbacks.ModelCheckpoint(
            nombre_checkpoint, monitor=monitor, mode="max", save_best_only=True
        ),
    ]

# ----------------------------------------------------------------------
# 7. EVALUACION FINAL
# ----------------------------------------------------------------------
def evaluar(modelo: tf.keras.Model, ds: tf.data.Dataset, df: pd.DataFrame, umbral: float = 0.5, etiqueta: str = "prueba"):
    y_true = df["clase"].map(CLASE_A_INDICE).values
    y_prob = modelo.predict(ds, verbose=0).ravel()
    y_pred = (y_prob >= umbral).astype(int)
    print(f"\n=== Reporte de clasificacion (conjunto de {etiqueta}, umbral={umbral:.3f}) ===")
    print(classification_report(y_true, y_pred, target_names=["sano", "infestado"]))
    print("Matriz de confusion (filas=real, columnas=predicho):")
    print(confusion_matrix(y_true, y_pred))

# ----------------------------------------------------------------------
# 7.5 BUSQUEDA DEL UMBRAL OPTIMO (NO ASUMIR 0.5)
# ----------------------------------------------------------------------
def calcular_umbral_optimo(modelo: tf.keras.Model, ds_val: tf.data.Dataset, y_val: np.ndarray, beta: float = 0.7) -> float:
    """
    Busca el umbral que maximiza F-beta en el set de VALIDACION (nunca en
    test, para no contaminar la evaluacion final).

    beta < 1 pondera mas la precision que el recall -- preferimos un
    modelo mas conservador para declarar "infestado", que es justo el
    comportamiento que se busca corregir. beta=1.0 seria F1 estandar.
    """
    y_prob = modelo.predict(ds_val, verbose=0).ravel()
    precisiones, recalls, umbrales = precision_recall_curve(y_val, y_prob)
    # precision_recall_curve regresa un punto extra al final sin umbral asociado
    precisiones, recalls = precisiones[:-1], recalls[:-1]

    with np.errstate(divide="ignore", invalid="ignore"):
        f_beta_scores = (
            (1 + beta**2) * precisiones * recalls / (beta**2 * precisiones + recalls)
        )
    f_beta_scores = np.nan_to_num(f_beta_scores)

    mejor_idx = int(np.argmax(f_beta_scores))
    mejor_umbral = float(umbrales[mejor_idx])

    print(
        f"\nUmbral optimo en validacion (beta={beta}): {mejor_umbral:.3f} "
        f"(precision={precisiones[mejor_idx]:.3f}, recall={recalls[mejor_idx]:.3f}, "
        f"F-beta={f_beta_scores[mejor_idx]:.3f})"
    )
    print("Como referencia, con umbral=0.5 fijo en el mismo set de validacion:")
    y_pred_05 = (y_prob >= 0.5).astype(int)
    print(classification_report(y_val, y_pred_05, target_names=["sano", "infestado"]))

    return mejor_umbral

# ----------------------------------------------------------------------
# 8. TRIAGE DE ETIQUETAS DUDOSAS
# ----------------------------------------------------------------------
def triage_etiquetas_dudosas(modelo: tf.keras.Model, df_train: pd.DataFrame, umbral: float = 0.5, top_n: int = 150):
    ds = construir_tfdataset(df_train, entrenamiento=False)
    y_true = df_train["clase"].map(CLASE_A_INDICE).values
    y_prob = modelo.predict(ds, verbose=0).ravel()

    df_out = df_train.copy()
    df_out["prob_infestado"] = y_prob
    df_out["prediccion"] = (y_prob >= umbral).astype(int)
    df_out["etiqueta_real"] = y_true
    df_out["es_error"] = df_out["prediccion"] != df_out["etiqueta_real"]
    df_out["confianza_del_error"] = np.where(
        df_out["prediccion"] == 1, df_out["prob_infestado"], 1 - df_out["prob_infestado"]
    )

    sospechosas = (
        df_out[df_out["es_error"]].sort_values("confianza_del_error", ascending=False).head(top_n)
    )
    sospechosas.to_csv("candidatos_revision_manual.csv", index=False)
    print(f"\n{len(sospechosas)} imagenes marcadas como candidatas a revision manual.")
    print("Guardadas en candidatos_revision_manual.csv, ordenadas por confianza del error.")

# ----------------------------------------------------------------------
# 9. EXPORTACION A TFLITE
# ----------------------------------------------------------------------
def exportar_tflite(modelo: tf.keras.Model, ruta_salida: str, umbral_recomendado: Optional[float] = None):
    converter = tf.lite.TFLiteConverter.from_keras_model(modelo)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()

    with open(ruta_salida, "wb") as f:
        f.write(tflite_model)

    tam_mb = len(tflite_model) / (1024 * 1024)
    print(f"\nModelo exportado a {ruta_salida} ({tam_mb:.2f} MB)")
    print("Vuelve a evaluar precision/recall con el modelo YA cuantizado antes")
    print("de darlo por bueno -- a veces cambia ligeramente respecto al original.")

    if umbral_recomendado is not None:
        print(
            f"\nIMPORTANTE: el .tflite solo emite la probabilidad cruda (sigmoid). "
            f"El umbral optimo encontrado en validacion fue {umbral_recomendado:.3f}, "
            f"NO 0.5. Guarda este numero junto con el modelo -- la app/backend debe "
            f"comparar la probabilidad contra este umbral, o el sesgo hacia "
            f"'infestado' puede reaparecer en produccion aunque el modelo este bien "
            f"calibrado."
        )

# ----------------------------------------------------------------------
# 10. FLUJO PRINCIPAL
# ----------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data_dir", type=Path, default=Path("dataset"))
    parser.add_argument("--manifest", type=Path, default=Path("manifest.csv"))
    parser.add_argument("--epocas_fase1", type=int, default=10)
    parser.add_argument("--epocas_fase2", type=int, default=30)
    parser.add_argument(
        "--descongelar_desde",
        type=int,
        default=250,
        help=(
            "EfficientNet-B3 tiene varios cientos de capas (cerca de 384). "
            "Con un dataset todavia limitado (~6k imagenes en total), "
            "descongelar menos capas (numero mas alto) reduce el riesgo de "
            "sobreajuste comparado con descongelar desde una capa muy temprana."
        ),
    )
    parser.add_argument(
        "--suavizado_pesos",
        type=float,
        default=0.6,
        help="0.0 = sin ponderar clases, 1.0 = balanceo completo de sklearn. Ver calcular_class_weights().",
    )
    parser.add_argument(
        "--beta",
        type=float,
        default=0.7,
        help=(
            "Controla el F-beta usado para elegir checkpoint y umbral final. "
            "beta < 1 favorece precision (menos falsos 'infestado'); "
            "beta = 1 es F1 estandar; beta > 1 favorece recall."
        ),
    )
    parser.add_argument("--dropout", type=float, default=0.3)
    args = parser.parse_args()

    df = construir_dataframe(args.data_dir, args.manifest)
    df_train, df_val, df_test = dividir_dataset(df)

    ds_train = construir_tfdataset(df_train, entrenamiento=True)
    ds_val = construir_tfdataset(df_val, entrenamiento=False)
    ds_test = construir_tfdataset(df_test, entrenamiento=False)
    y_val = df_val["clase"].map(CLASE_A_INDICE).values.astype("int32")

    class_weight = calcular_class_weights(df_train, suavizado=args.suavizado_pesos)

    print("\n=== FASE 1: entrenando solo el head (base congelada) ===")
    modelo = construir_modelo(fine_tune_from=None, dropout=args.dropout)
    compilar(modelo, learning_rate=1e-3)
    modelo.fit(
        ds_train,
        validation_data=ds_val,
        epochs=args.epocas_fase1,
        class_weight=class_weight,
        callbacks=callbacks_para("mejor_modelo_fase1.keras", ds_val, y_val, beta=args.beta),
    )

    print("\n--- Chequeo rapido tras fase 1 (antes de fine-tuning) ---")
    evaluar(modelo, ds_val, df_val, umbral=0.5, etiqueta="validacion")

    print("\n=== FASE 2: fine-tuning con la base parcialmente descongelada ===")
    modelo_ft = construir_modelo(fine_tune_from=args.descongelar_desde, dropout=args.dropout)
    modelo_ft.set_weights(modelo.get_weights())
    compilar(modelo_ft, learning_rate=1e-5)
    modelo_ft.fit(
        ds_train,
        validation_data=ds_val,
        epochs=args.epocas_fase2,
        class_weight=class_weight,
        callbacks=callbacks_para("mejor_modelo_fase2.keras", ds_val, y_val, beta=args.beta),
    )

    umbral_optimo = calcular_umbral_optimo(modelo_ft, ds_val, y_val, beta=args.beta)
    evaluar(modelo_ft, ds_test, df_test, umbral=umbral_optimo, etiqueta="prueba")
    triage_etiquetas_dudosas(modelo_ft, df_train, umbral=umbral_optimo)
    exportar_tflite(modelo_ft, "bark_classifier.tflite", umbral_recomendado=umbral_optimo)


if __name__ == "__main__":
    main()
