package com.karcliq.rag.service.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karcliq.rag.model.v2.RagV2AnswerResponse.RerankedChunkInfo;
import com.karcliq.rag.model.v2.RerankStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Two-stage Reranking Service for RAG V2.
 * Supports:
 * 1. CROSS_ENCODER: Neural cross-attention re-scoring between query and candidate documents.
 * 2. HYBRID: Rule-based vector cosine + lexical overlap + MMR source diversity.
 */
@Slf4j
@Service
public class RerankingService {

    private static final Set<String> STOP_WORDS = Set.of(
            "what", "are", "the", "in", "a", "an", "is", "for", "to", "of", "and", "or",
            "how", "do", "does", "with", "associated", "between", "on", "by", "at", "from"
    );

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public RerankingService() {
        this.chatModel = null;
        this.objectMapper = new ObjectMapper();
    }

    @Autowired
    public RerankingService(@Autowired(required = false) ChatModel chatModel) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
    }

    public record RerankResult(
            List<Document> selectedDocuments,
            List<RerankedChunkInfo> rerankedInfo
    ) {}

    /**
     * Default overload defaulting to CROSS_ENCODER for backward compatibility.
     */
    public RerankResult rerank(String query, List<Document> candidates, int topK) {
        return rerank(query, candidates, topK, RerankStrategy.CROSS_ENCODER);
    }

    /**
     * Reranks candidates using the selected strategy.
     */
    public RerankResult rerank(String query, List<Document> candidates, int topK, RerankStrategy strategy) {
        log.info("Reranking {} candidate chunks using strategy '{}' for query: '{}' (topK={})",
                candidates.size(), strategy, query, topK);

        if (candidates.isEmpty()) {
            return new RerankResult(Collections.emptyList(), Collections.emptyList());
        }

        if (strategy == RerankStrategy.CROSS_ENCODER) {
            return rerankWithCrossEncoder(query, candidates, topK);
        } else {
            return rerankHybrid(query, candidates, topK);
        }
    }

    /**
     * Neural Cross-Encoder Reranker.
     * Evaluates joint (Query ⨁ Passage) cross-attention using deep reasoning,
     * penalizing vehicle platform mismatches and rewarding specific pinouts/tolerances.
     */
    public RerankResult rerankWithCrossEncoder(String query, List<Document> candidates, int topK) {
        if (chatModel == null) {
            log.warn("ChatModel is not available for neural Cross-Encoder. Falling back to Hybrid reranking.");
            return rerankHybrid(query, candidates, topK);
        }

        try {
            StringBuilder candidatesText = new StringBuilder();
            Map<String, Document> docMap = new LinkedHashMap<>();
            Map<String, Double> initialVecScores = new HashMap<>();

            for (int i = 0; i < candidates.size(); i++) {
                Document doc = candidates.get(i);
                docMap.put(doc.getId(), doc);

                double vecScore = 0.5;
                if (doc.getMetadata().containsKey("distance")) {
                    Object dist = doc.getMetadata().get("distance");
                    if (dist instanceof Number n) {
                        vecScore = Math.max(0.0, 1.0 - n.doubleValue());
                    }
                }
                initialVecScores.put(doc.getId(), vecScore);

                String src = (String) doc.getMetadata().getOrDefault("source", "Manual");
                String contentSnippet = doc.getText() != null ? doc.getText().trim() : "";
                if (contentSnippet.length() > 600) {
                    contentSnippet = contentSnippet.substring(0, 600) + "...";
                }

                candidatesText.append(String.format("### Candidate [%d]\nID: %s\nSource: %s\nContent:\n%s\n\n",
                        i + 1, doc.getId(), src, contentSnippet));
            }

            String systemPrompt = """
                    You are a neural Cross-Encoder reranker specializing in OEM automotive workshop diagnostic manuals.
                    Your objective is to evaluate the joint semantic relevance of each candidate document passage to the technician's specific query.
                    
                    Evaluation Protocol:
                    1. Platform Specificity: Heavily penalize passages from the wrong vehicle model or engine architecture. (e.g. If the query asks about Mercedes-Benz C-Class / C 300 / M254, an S-Class / S 500 passage must receive a significantly lower score unless a comparison is explicitly requested).
                    2. Technical Precision: Heavily reward exact ECU connector pin numbers, electrical multimeter specs (resistance, voltage, duty cycle), and verified repair sequences.
                    3. Semantic Alignment: Reward passages addressing the exact root cause or symptoms in the query.
                    
                    Output format:
                    Return ONLY a JSON array of objects representing all candidates, ranked from most relevant to least relevant:
                    [
                      {"id": "doc-id-1", "score": 0.95},
                      {"id": "doc-id-2", "score": 0.32}
                    ]
                    Every score must be a number between 0.00 and 1.00.
                    """;

            String userPrompt = String.format("TECHNICIAN QUERY:\n\"%s\"\n\nCANDIDATES TO RERANK:\n%s",
                    query, candidatesText);

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt)
            ));

            log.info("Invoking neural Cross-Encoder via ChatModel for {} candidates...", candidates.size());
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            // Extract JSON array
            String jsonStr = response.trim();
            if (jsonStr.contains("```json")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
                if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                }
            } else if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
                if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                }
            }
            jsonStr = jsonStr.trim();

            JsonNode rootNode = objectMapper.readTree(jsonStr);
            JsonNode arrayNode = rootNode.isArray() ? rootNode : (rootNode.has("rankedResults") ? rootNode.get("rankedResults") : null);

            if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) {
                log.warn("Cross-Encoder output did not contain a valid JSON array. Falling back to Hybrid reranking.");
                return rerankHybrid(query, candidates, topK);
            }

            List<ScoredCandidate> scoredList = new ArrayList<>();
            Set<String> seenIds = new HashSet<>();

            for (JsonNode item : arrayNode) {
                String docId = item.path("id").asText();
                double crossScore = item.path("score").asDouble(0.5);
                Document doc = docMap.get(docId);
                if (doc != null && !seenIds.contains(docId)) {
                    seenIds.add(docId);
                    int initialRank = candidates.indexOf(doc) + 1;
                    double initScore = initialVecScores.getOrDefault(docId, 0.5);
                    String source = (String) doc.getMetadata().getOrDefault("source", "UNKNOWN");
                    scoredList.add(new ScoredCandidate(doc, initialRank, initScore, crossScore, source));
                }
            }

            // Append any unranked candidates at the end
            for (Document doc : candidates) {
                if (!seenIds.contains(doc.getId())) {
                    int initialRank = candidates.indexOf(doc) + 1;
                    double initScore = initialVecScores.getOrDefault(doc.getId(), 0.5);
                    String source = (String) doc.getMetadata().getOrDefault("source", "UNKNOWN");
                    scoredList.add(new ScoredCandidate(doc, initialRank, initScore, 0.20, source));
                }
            }

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
                        Math.round(sc.finalScore * 1000.0) / 1000.0,
                        snippet
                ));
            }

            log.info("Neural Cross-Encoder reranking completed. Top {} chunks selected.", topDocs.size());
            return new RerankResult(topDocs, infoList);

        } catch (Exception e) {
            log.error("Neural Cross-Encoder execution failed: {}. Falling back to Hybrid reranking.", e.getMessage());
            return rerankHybrid(query, candidates, topK);
        }
    }

    /**
     * Hybrid Search Reranker.
     * Rule-based combination of Vector Cosine + Query Keyword Overlap + MMR Source Diversity.
     */
    public RerankResult rerankHybrid(String query, List<Document> candidates, int topK) {
        log.info("Executing Hybrid Reranking (Vector + Lexical + Diversity) for query: '{}'", query);

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

            // 2. Keyword density / Lexical overlap
            double termScore = calculateTermOverlap(queryTerms, doc.getText(), source);

            // 3. Source diversity bonus
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
