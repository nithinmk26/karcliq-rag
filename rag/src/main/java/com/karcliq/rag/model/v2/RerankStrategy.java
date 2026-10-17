package com.karcliq.rag.model.v2;

/**
 * Strategy for the second-stage reranking in RAG V2.
 */
public enum RerankStrategy {
    /**
     * True neural cross-attention reranking evaluating query and passage tokens jointly.
     */
    CROSS_ENCODER,

    /**
     * Hybrid scoring combining vector cosine, lexical keyword overlap, and source diversity.
     */
    HYBRID
}
