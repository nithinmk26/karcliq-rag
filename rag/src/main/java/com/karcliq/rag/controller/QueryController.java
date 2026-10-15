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

    @Operation(
            summary = "1. Retrieval Only: Query document chunks (Top K)",
            description = "Performs cosine similarity search against PGVector embeddings using the query string. " +
                    "Returns the top K most relevant text chunks along with metadata, without reranking or LLM generation."
    )
    @GetMapping("/query")
    public ResponseEntity<QueryResponse> query(
            @Parameter(
                    description = "Diagnostic question or search query",
                    example = "What are the symptoms of DTC P0101 in Mercedes-Benz C-Class?"
            )
            @RequestParam String query,

            @Parameter(
                    description = "Number of top matching chunks to retrieve",
                    example = "4"
            )
            @RequestParam(defaultValue = "4", required = false) int topK,

            @Parameter(
                    description = "Optional minimum similarity threshold (leave empty to retrieve all top-K matches)",
                    required = false
            )
            @RequestParam(required = false) Double similarityThreshold
    ) {
        log.info("Received V1 retrieval query: '{}', topK: {}, threshold: {}", query, topK, similarityThreshold);
        QueryResponse response = retrievalService.retrieve(query, topK, similarityThreshold);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Full RAG Augmentation + Generation endpoint.
     */
    @Operation(
            summary = "2. RAG (Augment + Generate): Ask question and get grounded answer",
            description = "Retrieves relevant chunks from PGVector, injects them into an augmented token-efficient prompt, " +
                    "and queries OpenAI (gpt-4o-mini) to produce a precise answer citing source documents."
    )
    @GetMapping("/rag/query")
    public ResponseEntity<RagAnswerResponse> ragQuery(
            @Parameter(
                    description = "Diagnostic question to ask",
                    example = "What are the primary symptoms associated with DTC P0101 in a Mercedes-Benz?"
            )
            @RequestParam String query,

            @Parameter(
                    description = "Number of context chunks to retrieve and inject",
                    example = "4"
            )
            @RequestParam(defaultValue = "4", required = false) int topK,

            @Parameter(
                    description = "Optional minimum similarity threshold (leave empty to retrieve all top-K matches)",
                    required = false
            )
            @RequestParam(required = false) Double similarityThreshold
    ) {
        log.info("Received V1 RAG Query: '{}', topK: {}, threshold: {}", query, topK, similarityThreshold);
        RagAnswerResponse response = augmentationService.ask(query, topK, similarityThreshold);
        return ResponseEntity.ok(response);
    }
}
