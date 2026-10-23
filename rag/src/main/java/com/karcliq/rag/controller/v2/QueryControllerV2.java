package com.karcliq.rag.controller.v2;

import com.karcliq.rag.model.v2.RagV2AnswerResponse;
import com.karcliq.rag.service.v2.AugmentationServiceV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "RAG V2 (Reranking)")
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class QueryControllerV2 {

    private final AugmentationServiceV2 augmentationServiceV2;

    @Operation(summary = "V2 RAG with Reranking: Broad Retrieval + Choice of Cross-Encoder or Hybrid Search + LLM Generation")
    @GetMapping("/rag/query")
    public ResponseEntity<RagV2AnswerResponse> ragQueryV2(
            @Parameter(description = "Diagnostic question to ask")
            @RequestParam String query,

            @Parameter(description = "Number of top reranked chunks to feed into the LLM context")
            @RequestParam(defaultValue = "4", required = false) int topK,

            @Parameter(description = "Broad candidate pool size to retrieve in Stage 1 before reranking")
            @RequestParam(defaultValue = "10", required = false) int initialPoolSize,

            @Parameter(description = "Reranking strategy: CROSS_ENCODER (neural cross-attention) or HYBRID (vector + lexical + diversity)")
            @RequestParam(defaultValue = "CROSS_ENCODER", required = false) com.karcliq.rag.model.v2.RerankStrategy rerankStrategy
    ) {
        log.info("Received V2 RAG Query: '{}', topK: {}, poolSize: {}, strategy: {}", query, topK, initialPoolSize, rerankStrategy);
        RagV2AnswerResponse response = augmentationServiceV2.askWithRerank(query, topK, initialPoolSize, rerankStrategy);
        return ResponseEntity.ok(response);
    }
}
