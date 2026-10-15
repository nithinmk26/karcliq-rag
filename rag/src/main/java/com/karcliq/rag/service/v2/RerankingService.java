package com.karcliq.rag.service.v2;

import com.karcliq.rag.model.v2.RagV2AnswerResponse.RerankedChunkInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Two-stage Reranking Service for RAG V2.
 * Re-scores initial PGVector vector candidates by combining:
 * 1. Vector Cosine Similarity
 * 2. Query Term Overlap / Keyword Density
 * 3. Cross-Document Diversity (MMR-style source diversity balancing)
 */
@Slf4j
@Service
public class RerankingService {

    private static final Set<String> STOP_WORDS = Set.of(
            "what", "are", "the", "in", "a", "an", "is", "for", "to", "of", "and", "or",
            "how", "do", "does", "with", "associated", "between", "on", "by", "at", "from"
    );

    public record RerankResult(
            List<Document> selectedDocuments,
            List<RerankedChunkInfo> rerankedInfo
    ) {}

    public RerankResult rerank(String query, List<Document> candidates, int topK) {
        log.info("Reranking {} candidate chunks for query: '{}' (selecting top {})",
                candidates.size(), query, topK);

        if (candidates.isEmpty()) {
            return new RerankResult(Collections.emptyList(), Collections.emptyList());
        }

        // Extract key search terms from user query
        Set<String> queryTerms = extractKeywords(query);

        List<ScoredCandidate> scoredList = new ArrayList<>();
        Map<String, Integer> sourceCount = new HashMap<>();

        for (int i = 0; i < candidates.size(); i++) {
            Document doc = candidates.get(i);
            String source = (String) doc.getMetadata().getOrDefault("source", "UNKNOWN");

            // 1. Vector Cosine Similarity (0.0 to 1.0)
            double vecScore = 0.5;
            if (doc.getMetadata().containsKey("distance")) {
                Object dist = doc.getMetadata().get("distance");
                if (dist instanceof Number n) {
                    vecScore = Math.max(0.0, 1.0 - n.doubleValue());
                }
            }

            // 2. Keyword density / Lexical overlap (including source filename and word stemming)
            double termScore = calculateTermOverlap(queryTerms, doc.getText(), source);

            // 3. Source diversity bonus:
            int countFromSource = sourceCount.getOrDefault(source, 0);
            double diversityBonus = countFromSource == 0 ? 0.06 : (countFromSource == 1 ? 0.02 : 0.0);
            sourceCount.put(source, countFromSource + 1);

            // Combined hybrid rerank score (50% vector, 40% lexical match, 10% platform balance)
            double finalScore = (vecScore * 0.50) + (termScore * 0.40) + diversityBonus;
            finalScore = Math.round(finalScore * 1000.0) / 1000.0;

            scoredList.add(new ScoredCandidate(doc, i + 1, vecScore, finalScore, source));
        }

        // Sort descending by final rerank score
        scoredList.sort((a, b) -> Double.compare(b.finalScore, a.finalScore));

        List<Document> topDocs = new ArrayList<>();
        List<RerankedChunkInfo> infoList = new ArrayList<>();

        int limit = Math.min(topK, scoredList.size());
        for (int rank = 0; rank < limit; rank++) {
            ScoredCandidate sc = scoredList.get(rank);
            topDocs.add(sc.document);

            String snippet = sc.document.getText() != null
                    ? (sc.document.getText().length() > 140
                    ? sc.document.getText().substring(0, 140).replace("\n", " ") + "..."
                    : sc.document.getText().replace("\n", " "))
                    : "";

            infoList.add(new RerankedChunkInfo(
                    sc.document.getId(),
                    sc.source,
                    sc.initialRank,
                    rank + 1,
                    Math.round(sc.initialVecScore * 1000.0) / 1000.0,
                    sc.finalScore,
                    snippet
            ));
        }

        log.info("Reranking completed. Top {} chunks selected across {} unique sources",
                topDocs.size(), topDocs.stream().map(d -> d.getMetadata().get("source")).distinct().count());

        return new RerankResult(topDocs, infoList);
    }

    private Set<String> extractKeywords(String query) {
        Set<String> keywords = new HashSet<>();
        for (String raw : query.toLowerCase().split("[^a-zA-Z0-9]+")) {
            if (!raw.isBlank() && !STOP_WORDS.contains(raw) && raw.length() > 2) {
                keywords.add(raw);
            }
        }
        return keywords;
    }

    private double calculateTermOverlap(Set<String> keywords, String text, String source) {
        if (keywords.isEmpty()) {
            return 0.0;
        }
        String combined = ((text != null ? text : "") + " " + (source != null ? source : "")).toLowerCase();
        long matches = 0;
        for (String kw : keywords) {
            if (combined.contains(kw)) {
                matches++;
            } else if (kw.endsWith("es") && kw.length() > 4 && combined.contains(kw.substring(0, kw.length() - 2))) {
                matches++;
            } else if (kw.endsWith("s") && kw.length() > 3 && combined.contains(kw.substring(0, kw.length() - 1))) {
                matches++;
            }
        }
        return (double) matches / keywords.size();
    }

    private record ScoredCandidate(
            Document document,
            int initialRank,
            double initialVecScore,
            double finalScore,
            String source
    ) {}
}
