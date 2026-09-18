package com.descortezadores.adapters.controllers;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.time.Instant;
import java.util.Map;

@Controller("/api/v1/health")
@Secured(SecurityRule.IS_ANONYMOUS)
public class HealthController {

    @Get
    public HttpResponse<Map<String, Object>> health() {
        return HttpResponse.ok(Map.of(
            "status", "UP",
            "service", "backend-descortezadores",
            "timestamp", Instant.now().toString(),
            "version", "1.0.0"
        ));
    }
}
