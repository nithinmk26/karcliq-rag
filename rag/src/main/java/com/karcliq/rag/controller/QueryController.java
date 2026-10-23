package com.karcliq.rag.controller;

import com.karcliq.rag.model.QueryResponse;
import com.karcliq.rag.model.RagAnswerResponse;
import com.karcliq.rag.service.AugmentationService;
import com.karcliq.rag.service.RetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "RAG V1", description = "V1 API endpoints for Retrieval, Augmentation, and Generation")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QueryController {

    private final RetrievalService retrievalService;
    private final AugmentationService augmentationService;

    @Operation(summary = " Retrieval Only: Query document chunks (Top K)")
    @GetMapping("/query")
    public ResponseEntity<QueryResponse> query(
            @Parameter(description = "Diagnostic question or search query")
            @RequestParam String query,

            @Parameter(description = "Number of top matching chunks to retrieve")
            @RequestParam(defaultValue = "4", required = false) int topK,

            @Parameter(description = "Optional minimum similarity threshold (leave empty to retrieve all top-K matches)")
            @RequestParam(required = false) Double similarityThreshold
    ) {
        log.info("Received V1 retrieval query: '{}', topK: {}, threshold: {}", query, topK, similarityThreshold);
        QueryResponse response = retrievalService.retrieve(query, topK, similarityThreshold);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "RAG Ask question and get grounded answer")
    @GetMapping("/rag/query")
    public ResponseEntity<RagAnswerResponse> ragQuery(
            @Parameter(description = "Diagnostic question to ask")
            @RequestParam String query,

            @Parameter(description = "Number of context chunks to retrieve and inject")
            @RequestParam(defaultValue = "4", required = false) int topK,

            @Parameter(description = "Optional minimum similarity threshold (leave empty to retrieve all top-K matches)")
            @RequestParam(required = false) Double similarityThreshold
    ) {
        log.info("Received V1 RAG Query: '{}', topK: {}, threshold: {}", query, topK, similarityThreshold);
        RagAnswerResponse response = augmentationService.ask(query, topK, similarityThreshold);
        return ResponseEntity.ok(response);
    }
}
