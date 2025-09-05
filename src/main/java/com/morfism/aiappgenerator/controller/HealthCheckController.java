package com.morfism.aiappgenerator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple health endpoint for readiness/liveness probes and platform checks.
 */
@RestController
public class HealthCheckController {

    @GetMapping("/health")
    public String health() {
        // Return plain text to keep it simple for generic platform probes
        return "OK";
    }
}
