package com.karcliq.rag.model;

/**
 * Chunking strategies for PDF processing.
 */
public enum ChunkingStrategy {
    FIXED_SIZE_WITH_OVERLAP,
    SLIDING_WINDOW,
    RECURSIVE,
    SECTION_BASED
}
