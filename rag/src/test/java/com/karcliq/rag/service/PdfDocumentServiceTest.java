package com.karcliq.rag.service;

import com.karcliq.rag.model.ChunkingStrategy;
import com.karcliq.rag.model.PdfProcessingResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PdfDocumentServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private PdfDocumentService pdfDocumentService;

    // 1. Positive Test: Process PDFs from valid existing folder
    @Test
    void testProcessPdfFolder_Positive() {
        PdfProcessingResponse response = pdfDocumentService.processPdfFolder(
                "c:\\AI\\automotive_diagnostic",
                ChunkingStrategy.RECURSIVE,
                1000,
                200,
                false
        );

        assertNotNull(response);
        assertTrue(response.totalFiles() > 0);
        assertTrue(response.totalChunks() > 0);
        assertEquals(ChunkingStrategy.RECURSIVE, response.strategy());
    }

    // 2. Negative Test: Throws exception for invalid/missing folder path
    @Test
    void testProcessPdfFolder_Negative() {
        assertThrows(IllegalArgumentException.class, () ->
                pdfDocumentService.processPdfFolder(
                        "c:\\invalid\\path\\that\\does\\not\\exist",
                        ChunkingStrategy.RECURSIVE,
                        1000,
                        200,
                        false
                )
        );
    }
}
