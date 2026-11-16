import os
import json
import time
import base64
import io
import numpy as np
from PIL import Image
from requests_toolbelt.multipart import decoder
from ai_edge_litert.interpreter import Interpreter

MODEL_PATH = os.environ.get("MODEL_PATH", "classifier_full.tflite")
IMG_SIZE = 300
UMBRAL_DECISION = float(os.environ.get("UMBRAL_DECISION", "0.5"))

print(f"Cargando modelo de alta precisión LiteRT desde: {MODEL_PATH}...")
t0 = time.time()
interpreter = Interpreter(model_path=MODEL_PATH)
interpreter.allocate_tensors()
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()
input_index = input_details[0]["index"]
output_index = output_details[0]["index"]
print(f"Modelo cargado y tensores preparados en {time.time() - t0:.2f}s")


def extraer_bytes_imagen(event):
    """Extrae los bytes binarios de la imagen desde el evento de API Gateway."""
    body = event.get("body", "")
    is_base64 = event.get("isBase64Encoded", False)

    if is_base64:
        raw_body = base64.b64decode(body)
    elif isinstance(body, str):
        raw_body = body.encode("utf-8")
    else:
        raw_body = body

    headers = {k.lower(): v for k, v in (event.get("headers") or {}).items()}
    content_type = headers.get("content-type", "")

    # Caso 1: multipart/form-data
    if "multipart/form-data" in content_type:
        multipart_data = decoder.MultipartDecoder(raw_body, content_type)
        for part in multipart_data.parts:
            cd = part.headers.get(b"Content-Disposition", b"").decode("utf-8", errors="ignore")
            if "filename" in cd or 'name="imagen"' in cd or 'name="file"' in cd:
                return part.content
        if multipart_data.parts:
            return multipart_data.parts[0].content

    # Caso 2: application/json con Base64
    if "application/json" in content_type:
        data = json.loads(raw_body.decode("utf-8"))
        b64_str = data.get("imagenBase64") or data.get("image") or data.get("foto") or data.get("archivo")
        if not b64_str:
            raise ValueError("El JSON debe contener el campo 'imagenBase64'")
        if "," in b64_str:
            b64_str = b64_str.split(",")[1]
        return base64.b64decode(b64_str)

    # Caso 3: Imagen binaria directa
    return raw_body


def preprocesar_y_clasificar(image_bytes):
    """Preprocesa la imagen a 300x300 en Float32 [0, 255] y corre la inferencia."""
    img_pil = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    ancho_orig, alto_orig = img_pil.size

    # Redimensionado bilineal a 300x300 sin normalización manual (EfficientNet-B3 normaliza internamente)
    img_resized = img_pil.resize((IMG_SIZE, IMG_SIZE), Image.Resampling.BILINEAR)
    img_array = np.array(img_resized, dtype=np.float32)
    img_final = np.expand_dims(img_array, axis=0)

    t_start = time.time()
    interpreter.set_tensor(input_index, img_final)
    interpreter.invoke()
    prob = float(interpreter.get_tensor(output_index)[0][0])
    tiempo_ms = int((time.time() - t_start) * 1000)

    resultado = "infestado" if prob >= UMBRAL_DECISION else "sano"
    confianza = prob if resultado == "infestado" else (1.0 - prob)

    return {
        "resultado": resultado,
        "confianza": round(confianza, 4),
        "probabilidadInfestado": round(prob, 4),
        "umbral": UMBRAL_DECISION,
        "tiempoInferenciaMs": max(tiempo_ms, 1),
        "dimensionesOriginales": f"{ancho_orig}x{alto_orig}",
        "modelo": "classifier_full (EfficientNet-B3 Full Precision)",
        "tipoInferencia": "ONLINE_CLOUD_FULL_PRECISION",
        "version": "1.0.0"
    }


def lambda_handler(event, context):
    """Punto de entrada de AWS Lambda."""
    cors_headers = {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "POST, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization, Accept, Origin, X-Requested-With"
    }

    http_method = event.get("requestContext", {}).get("http", {}).get("method") or event.get("httpMethod")
    if http_method == "OPTIONS":
        return {
            "statusCode": 204,
            "headers": cors_headers,
            "body": ""
        }

    try:
        image_bytes = extraer_bytes_imagen(event)
        if not image_bytes or len(image_bytes) < 100:
            return {
                "statusCode": 400,
                "headers": cors_headers,
                "body": json.dumps({"error": "No se recibió ninguna imagen válida en la petición"})
            }

        resultado_diagnostico = preprocesar_y_clasificar(image_bytes)

        return {
            "statusCode": 200,
            "headers": cors_headers,
            "body": json.dumps(resultado_diagnostico)
        }

    except Exception as e:
        print(f"Error procesando inferencia: {e}")
        import traceback
        traceback.print_exc()
        return {
            "statusCode": 500,
            "headers": cors_headers,
            "body": json.dumps({"error": f"Error en inferencia: {str(e)}"})
        }
