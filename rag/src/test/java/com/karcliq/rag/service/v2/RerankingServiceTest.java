package com.karcliq.rag.service.v2;

import com.karcliq.rag.service.v2.RerankingService.RerankResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RerankingServiceTest {

    private RerankingService rerankingService;

    @BeforeEach
    void setUp() {
        rerankingService = new RerankingService();
    }

    // 1. Positive Test: Reranks candidate documents successfully using HYBRID strategy
    @Test
    void testRerank_Hybrid_Positive() {
        Document doc1 = new Document("1", "DTC P0101 diagnostic procedure.", Map.of("source", "C_Class.pdf"));
        Document doc2 = new Document("2", "General transmission maintenance.", Map.of("source", "Trans.pdf"));

        RerankResult result = rerankingService.rerank("P0101 diagnostic", List.of(doc1, doc2), 2, com.karcliq.rag.model.v2.RerankStrategy.HYBRID);

        assertNotNull(result);
        assertEquals(2, result.selectedDocuments().size());
        assertEquals("1", result.selectedDocuments().get(0).getId());
    }

    // 1b. Positive Test: Reranks candidate documents with CROSS_ENCODER (falls back gracefully when chatModel is null)
    @Test
    void testRerank_CrossEncoder_Fallback_Positive() {
        Document doc1 = new Document("1", "DTC P0101 diagnostic procedure.", Map.of("source", "C_Class.pdf"));
        Document doc2 = new Document("2", "General transmission maintenance.", Map.of("source", "Trans.pdf"));

        RerankResult result = rerankingService.rerank("P0101 diagnostic", List.of(doc1, doc2), 2, com.karcliq.rag.model.v2.RerankStrategy.CROSS_ENCODER);

        assertNotNull(result);
        assertEquals(2, result.selectedDocuments().size());
        assertEquals("1", result.selectedDocuments().get(0).getId());
    }

    // 2. Negative Test: Handles empty candidate list gracefully
    @Test
    void testRerank_Negative() {
        RerankResult result = rerankingService.rerank("P0101", Collections.emptyList(), 4, com.karcliq.rag.model.v2.RerankStrategy.CROSS_ENCODER);

        assertNotNull(result);
        assertTrue(result.selectedDocuments().isEmpty());
        assertTrue(result.rerankedInfo().isEmpty());
    }
}
