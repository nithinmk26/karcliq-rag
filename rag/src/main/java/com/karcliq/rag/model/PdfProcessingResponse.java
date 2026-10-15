package com.karcliq.rag.model;

/**
 * Minimal summary response for PDF chunking.
 */
public record PdfProcessingResponse(
        int totalFiles,
        int totalChunks,
        ChunkingStrategy strategy,
        String message
) {}
