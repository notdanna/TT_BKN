FROM public.ecr.aws/lambda/python:3.12

# Instalar dependencias
COPY requirements.txt ${LAMBDA_TASK_ROOT}/
RUN pip install --default-timeout=100 --no-cache-dir -r ${LAMBDA_TASK_ROOT}/requirements.txt

# Copiar modelo de alta precision Float32 (42.8 MB) y codigo
COPY classifier_full.tflite ${LAMBDA_TASK_ROOT}/
COPY app.py ${LAMBDA_TASK_ROOT}/

CMD [ "app.lambda_handler" ]
