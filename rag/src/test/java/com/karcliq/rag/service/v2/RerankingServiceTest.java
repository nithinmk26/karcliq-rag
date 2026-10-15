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

    // 1. Positive Test: Reranks candidate documents successfully
    @Test
    void testRerank_Positive() {
        Document doc1 = new Document("1", "DTC P0101 diagnostic procedure.", Map.of("source", "C_Class.pdf"));
        Document doc2 = new Document("2", "General transmission maintenance.", Map.of("source", "Trans.pdf"));

        RerankResult result = rerankingService.rerank("P0101 diagnostic", List.of(doc1, doc2), 2);

        assertNotNull(result);
        assertEquals(2, result.selectedDocuments().size());
        assertEquals("1", result.selectedDocuments().get(0).getId());
    }

    // 2. Negative Test: Handles empty candidate list gracefully
    @Test
    void testRerank_Negative() {
        RerankResult result = rerankingService.rerank("P0101", Collections.emptyList(), 4);

        assertNotNull(result);
        assertTrue(result.selectedDocuments().isEmpty());
        assertTrue(result.rerankedInfo().isEmpty());
    }
}
