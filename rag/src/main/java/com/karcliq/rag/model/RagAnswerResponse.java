package com.karcliq.rag.model;

import java.util.List;

/**
 * Response DTO containing the augmented RAG answer, sources, and context statistics.
 */
public record RagAnswerResponse(
        String query,
        String answer,
        String modelUsed,
        List<String> sources,
        int chunksUsed,
        String augmentedPromptPreview
) {}
