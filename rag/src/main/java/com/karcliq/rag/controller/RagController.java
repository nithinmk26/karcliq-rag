package com.karcliq.rag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Status", description = "Health endpoints")
@RestController
@RequestMapping("/api")
public class RagController {

    public record AppStatusResponse(String status, String message) {}

    @Operation(summary = "Application status", description = "Returns simple health status of the RAG service")
    @ApiResponse(responseCode = "200", description = "Service is up")
    @GetMapping("/status")
    public ResponseEntity<AppStatusResponse> status() {
        log.info("Received request to check application status");
        AppStatusResponse response = new AppStatusResponse("UP", "Karcliq RAG service is running");
        log.debug("Returning status response: {}", response);
        return ResponseEntity.ok(response);
    }
}
