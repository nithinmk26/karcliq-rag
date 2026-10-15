package com.karcliq.rag.service;

import com.karcliq.rag.model.QueryResponse;
import com.karcliq.rag.model.QueryResponse.QueryResultItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service handling similarity search and document retrieval from PGVector.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final VectorStore vectorStore;

    /**
     * Performs standard cosine similarity search in PGVector without reranking.
     *
     * @param query               The user search string
     * @param topK                Number of top relevant chunks to retrieve
     * @param similarityThreshold Optional minimum similarity cutoff (e.g. 0.70)
     * @return QueryResponse containing retrieved chunks and metadata
     */
    public QueryResponse retrieve(String query, int topK, Double similarityThreshold) {
        log.info("Executing retrieval query: '{}' with topK: {}, threshold: {}", query, topK, similarityThreshold);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK);

        if (similarityThreshold != null && similarityThreshold > 0.0) {
            builder.similarityThreshold(similarityThreshold);
        }

        SearchRequest searchRequest = builder.build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        log.info("Retrieved {} matching document chunks from PGVector", documents.size());

        List<QueryResultItem> items = documents.stream()
                .map(doc -> {
                    Double score = null;
                    if (doc.getMetadata().containsKey("distance")) {
                        Object dist = doc.getMetadata().get("distance");
                        if (dist instanceof Number n) {
                            score = Math.round((1.0 - n.doubleValue()) * 1000.0) / 1000.0;
                        }
                    }
                    String source = (String) doc.getMetadata().getOrDefault("source", "UNKNOWN");
                    return new QueryResultItem(
                            doc.getId(),
                            score,
                            source,
                            doc.getMetadata(),
                            doc.getText()
                    );
                })
                .toList();

        return new QueryResponse(query, topK, items.size(), items);
    }
}
