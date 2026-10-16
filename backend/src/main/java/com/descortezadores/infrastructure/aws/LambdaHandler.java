package com.descortezadores.infrastructure.aws;

import io.micronaut.function.aws.proxy.payload1.ApiGatewayProxyRequestEventFunction;

/**
 * Handler oficial para AWS Lambda Serverless con AWS API Gateway (REST API o HTTP API con proxy v1).
 *
 * Configuración en la consola de AWS Lambda o plantilla AWS SAM / CDK:
 * - Handler: com.descortezadores.infrastructure.aws.LambdaHandler::handleRequest
 * - Runtime: Java 21 (Amazon Linux 2023 / Corretto 21)
 * - Arquitectura recomendada: arm64 (AWS Graviton2) para menor latencia y 20% menor costo
 * - Memoria recomendada: 512 MB (según presupuesto TT §4.4.2 y §5.4)
 * - Timeout recomendado: 15 a 30 segundos
 *
 * Funcionalidad:
 * Reutiliza el contexto de inyección de dependencias AoT de Micronaut compilado,
 * permitiendo ejecutar todos los controladores (@Controller), filtros de seguridad JWT,
 * transacciones y reglas de negocio exactamente igual que en un contenedor Netty local,
 * pero con escalado automático a cero y pago por uso estricto.
 */
public class LambdaHandler extends ApiGatewayProxyRequestEventFunction {

    public LambdaHandler() {
        super();
    }
}
