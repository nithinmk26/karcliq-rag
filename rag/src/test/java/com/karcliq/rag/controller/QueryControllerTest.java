package com.karcliq.rag.controller;

import com.karcliq.rag.model.QueryResponse;
import com.karcliq.rag.model.RagAnswerResponse;
import com.karcliq.rag.service.AugmentationService;
import com.karcliq.rag.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QueryControllerTest {

    @Mock
    private RetrievalService retrievalService;

    @Mock
    private AugmentationService augmentationService;

    @InjectMocks
    private QueryController queryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(queryController).build();
    }

    // 1. query() - Positive: Valid query param returns 200 OK
    @Test
    void testQuery_Positive() throws Exception {
        QueryResponse mockResponse = new QueryResponse("P0101", 4, 0, Collections.emptyList());
        when(retrievalService.retrieve(eq("P0101"), eq(4), isNull())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/query")
                        .param("query", "P0101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("P0101"));
    }

    // 1. query() - Negative: Missing required query param returns 400 Bad Request
    @Test
    void testQuery_Negative() throws Exception {
        mockMvc.perform(get("/api/v1/query"))
                .andExpect(status().isBadRequest());
    }

    // 2. ragQuery() - Positive: Valid query param returns 200 OK with answer
    @Test
    void testRagQuery_Positive() throws Exception {
        RagAnswerResponse mockResponse = new RagAnswerResponse("P0101", "Diagnostic answer", "gpt-4o-mini", List.of("guide.pdf"), 1, "prompt");
        when(augmentationService.ask(eq("P0101"), eq(4), isNull())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/rag/query")
                        .param("query", "P0101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Diagnostic answer"));
    }

    // 2. ragQuery() - Negative: Missing required query param returns 400 Bad Request
    @Test
    void testRagQuery_Negative() throws Exception {
        mockMvc.perform(get("/api/v1/rag/query"))
                .andExpect(status().isBadRequest());
    }
}
