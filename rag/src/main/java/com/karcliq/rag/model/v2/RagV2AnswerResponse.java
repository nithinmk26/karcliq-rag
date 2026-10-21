package com.karcliq.rag.model.v2;

import java.util.List;

/**
 * Response DTO for the V2 RAG endpoint with reranking metrics.
 */
public record RagV2AnswerResponse(
        String query,
        String answer,
        String modelUsed,
        List<String> sources,
        int initialCandidatesRetrieved,
        int topKSelected,
        List<RerankedChunkInfo> rerankedChunks,
        String augmentedPromptPreview,
        String rerankStrategy
) {
    public RagV2AnswerResponse(
            String query,
            String answer,
            String modelUsed,
            List<String> sources,
            int initialCandidatesRetrieved,
            int topKSelected,
            List<RerankedChunkInfo> rerankedChunks,
            String augmentedPromptPreview
    ) {
        this(query, answer, modelUsed, sources, initialCandidatesRetrieved, topKSelected, rerankedChunks, augmentedPromptPreview, "HYBRID");
    }

    public record RerankedChunkInfo(
            String id,
            String source,
            int initialRank,
            int rerankedRank,
            Double initialScore,
            Double rerankScore,
            String contentSnippet
    ) {}
}
