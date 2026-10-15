package com.karcliq.rag.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RagController ragController = new RagController();
        mockMvc = MockMvcBuilders.standaloneSetup(ragController).build();
    }

    // 1. Positive Test: GET /api/status returns 200 OK with UP status
    @Test
    void testStatus_Positive() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // 2. Negative Test: POST to a GET-only endpoint returns 405 Method Not Allowed
    @Test
    void testStatus_Negative_MethodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/status"))
                .andExpect(status().isMethodNotAllowed());
    }
}
