import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE" # Evita errores en Mac

import numpy as np
import pandas as pd
import tensorflow as tf
from pathlib import Path
from tqdm import tqdm
from PIL import Image

# ==========================================
# CONFIGURACIÓN
# ==========================================
TEST_DIR = Path("test") # Tu carpeta con imágenes de prueba
KERAS_MODEL_PATH = "mejor_modelo_fase2.keras" # El modelo original
TFLITE_MODEL_PATH = "bark_classifier.tflite"  # El modelo exportado para Android
IMG_SIZE = 300

# Evitar que TF reserve toda la memoria de golpe en Mac M2
gpus = tf.config.list_physical_devices("GPU")
if gpus:
    try:
        for _gpu in gpus:
            tf.config.experimental.set_memory_growth(_gpu, True)
    except RuntimeError:
        pass

# ==========================================
# FUNCIONES
# ==========================================
def preprocesar_imagen(ruta_imagen):
    # 1. Usamos PIL para cargar la imagen (soporta WebP, JPG, PNG sin crashear)
    img_pil = Image.open(ruta_imagen).convert("RGB")
    img_array_bruto = np.array(img_pil)

    # 2. Convertimos a tensor y redimensionamos con TF.
    # Esto garantiza que el algoritmo de resize sea idéntico al del entrenamiento.
    img_tensor = tf.convert_to_tensor(img_array_bruto, dtype=tf.float32)
    img_tensor = tf.image.resize(img_tensor, [IMG_SIZE, IMG_SIZE])

    # 3. EfficientNetB3 espera [0, 255]. NO normalizamos manualmente.
    img_final = tf.expand_dims(img_tensor, axis=0)

    return img_final.numpy()

def analizar_carpeta():
    # 1. Validaciones
    if not TEST_DIR.exists():
        print(f"⚠️ Error: No existe la carpeta '{TEST_DIR}'. Créala y mete imágenes ahí.")
        return
    if not Path(KERAS_MODEL_PATH).exists():
        print(f"⚠️ Error: No se encontró '{KERAS_MODEL_PATH}'.")
        return
    if not Path(TFLITE_MODEL_PATH).exists():
        print(f"⚠️ Error: No se encontró '{TFLITE_MODEL_PATH}'.")
        return

    # 2. Cargar modelo Keras completo
    print("Cargando modelo Keras (.keras)...")
    # custom_objects para suprimir cualquier error relacionado al optimizador legacy al cargar
    modelo_keras = tf.keras.models.load_model(KERAS_MODEL_PATH, compile=False)

    # 3. Cargar modelo TFLite (Simulando lo que hará Android)
    print("Cargando modelo TFLite (.tflite)...")
    interpreter = tf.lite.Interpreter(model_path=TFLITE_MODEL_PATH)
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    # 4. Obtener las imágenes
    extensiones = {".jpg", ".jpeg", ".png", ".webp"}
    rutas = [p for p in TEST_DIR.rglob("*") if p.suffix.lower() in extensiones]

    if not rutas:
        print(f"⚠️ No hay imágenes en la carpeta '{TEST_DIR}'.")
        return

    print(f"\nIniciando análisis de {len(rutas)} imágenes...\n")

    resultados = []

    # 5. Inferencia
    for ruta in tqdm(rutas, desc="Analizando"):
        img_array = preprocesar_imagen(ruta)

        # Inferencia con Keras
        prob_keras = modelo_keras.predict(img_array, verbose=0)[0][0]
        clase_keras = "Infestado" if prob_keras >= 0.5 else "Sano"

        # Inferencia con TFLite
        interpreter.set_tensor(input_details[0]['index'], img_array)
        interpreter.invoke()
        prob_tflite = interpreter.get_tensor(output_details[0]['index'])[0][0]
        clase_tflite = "Infestado" if prob_tflite >= 0.5 else "Sano"

        resultados.append({
            "Archivo": ruta.name,
            "Keras (%)": round(prob_keras * 100, 2),
            "Clase_Keras": clase_keras,
            "TFLite (%)": round(prob_tflite * 100, 2),
            "Clase_TFLite": clase_tflite,
            "Diferencia": round(abs(prob_keras - prob_tflite) * 100, 4)
        })

    # 6. Mostrar y guardar resultados
    df = pd.DataFrame(resultados)

    print("\n\n=== RESULTADOS DE INFERENCIA ===")
    print(df.to_string(index=False))

    df.to_csv("resultados_test.csv", index=False)
    print("\n✅ Análisis terminado. Los resultados detallados se guardaron en 'resultados_test.csv'")

if __name__ == "__main__":
    analizar_carpeta()
