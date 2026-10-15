package com.karcliq.rag.service;

import com.karcliq.rag.model.QueryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private RetrievalService retrievalService;

    // 1. Positive Test
    @Test
    void testRetrieve_Positive() {
        Document doc = new Document("1", "Diagnostic test content", Map.of("source", "manual.pdf", "distance", 0.2));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        QueryResponse response = retrievalService.retrieve("P0101", 4, 0.7);

        assertNotNull(response);
        assertEquals(1, response.totalFound());
        assertEquals("Diagnostic test content", response.results().get(0).content());
    }

    // 2. Negative Test
    @Test
    void testRetrieve_Negative() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        QueryResponse response = retrievalService.retrieve("invalid query", 4, null);

        assertNotNull(response);
        assertEquals(0, response.totalFound());
        assertTrue(response.results().isEmpty());
    }
}
