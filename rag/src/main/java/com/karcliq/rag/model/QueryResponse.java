package com.karcliq.rag.model;

import java.util.List;
import java.util.Map;

/**
 * Clean response DTO for the v1 retrieval query endpoint.
 */
public record QueryResponse(
        String query,
        int topK,
        int totalFound,
        List<QueryResultItem> results
) {
    public record QueryResultItem(
            String id,
            Double score,
            String source,
            Map<String, Object> metadata,
            String content
    ) {}
}
