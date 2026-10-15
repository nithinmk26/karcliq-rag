package com.karcliq.rag.controller;

import com.karcliq.rag.model.ChunkingStrategy;
import com.karcliq.rag.model.PdfProcessingResponse;
import com.karcliq.rag.service.PdfDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PdfDocumentControllerTest {

    @Mock
    private PdfDocumentService pdfDocumentService;

    @InjectMocks
    private PdfDocumentController pdfDocumentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pdfDocumentController).build();
    }

    // 1. Positive Test: POST /api/documents/process-pdfs returns 200 OK
    @Test
    void testProcessPdfs_Positive() throws Exception {
        PdfProcessingResponse mockResponse = new PdfProcessingResponse(
                10, 100, ChunkingStrategy.RECURSIVE, "Processed successfully"
        );
        when(pdfDocumentService.processPdfFolder(anyString(), any(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/documents/process-pdfs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles").value(10));
    }

    // 2. Negative Test: POST with invalid strategy parameter returns 400 Bad Request
    @Test
    void testProcessPdfs_Negative() throws Exception {
        mockMvc.perform(post("/api/documents/process-pdfs")
                        .param("strategy", "INVALID_CHUNK_STRATEGY"))
                .andExpect(status().isBadRequest());
    }
}
