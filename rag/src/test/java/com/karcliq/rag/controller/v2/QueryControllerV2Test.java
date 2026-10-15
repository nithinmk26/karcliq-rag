package com.karcliq.rag.controller.v2;

import com.karcliq.rag.model.v2.RagV2AnswerResponse;
import com.karcliq.rag.service.v2.AugmentationServiceV2;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QueryControllerV2Test {

    @Mock
    private AugmentationServiceV2 augmentationServiceV2;

    @InjectMocks
    private QueryControllerV2 queryControllerV2;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(queryControllerV2).build();
    }

    // 1. Positive Test: API call with query param returns 200 OK
    @Test
    void testRagQueryV2_Positive() throws Exception {
        RagV2AnswerResponse mockResponse = new RagV2AnswerResponse(
                "P0101", "V2 Diagnostic Answer", "gpt-4o-mini", List.of("guide.pdf"), 10, 1, Collections.emptyList(), "prompt"
        );
        when(augmentationServiceV2.askWithRerank("P0101", 4, 10)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v2/rag/query")
                        .param("query", "P0101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("V2 Diagnostic Answer"));
    }

    // 2. Negative Test: Missing required query param returns 400 Bad Request
    @Test
    void testRagQueryV2_Negative() throws Exception {
        mockMvc.perform(get("/api/v2/rag/query"))
                .andExpect(status().isBadRequest());
    }
}
