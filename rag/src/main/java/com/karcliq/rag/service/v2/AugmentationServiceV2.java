package com.karcliq.rag.service.v2;

import com.karcliq.rag.model.v2.RagV2AnswerResponse;
import com.karcliq.rag.service.v2.RerankingService.RerankResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service implementing the V2 RAG Pipeline with two-stage Retrieval & Reranking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AugmentationServiceV2 {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final RerankingService rerankingService;

    @Value("${spring.ai.openai.model:gpt-4o-mini}")
    private String modelName;

    public RagV2AnswerResponse askWithRerank(String query, int topK, int initialPoolSize) {
        return askWithRerank(query, topK, initialPoolSize, com.karcliq.rag.model.v2.RerankStrategy.CROSS_ENCODER);
    }

    public RagV2AnswerResponse askWithRerank(String query, int topK, int initialPoolSize, com.karcliq.rag.model.v2.RerankStrategy rerankStrategy) {
        com.karcliq.rag.model.v2.RerankStrategy strategy = rerankStrategy != null ? rerankStrategy : com.karcliq.rag.model.v2.RerankStrategy.CROSS_ENCODER;
        log.info("V2 RAG Pipeline: query='{}', topK={}, poolSize={}, strategy={}", query, topK, initialPoolSize, strategy);

        int effectivePoolSize = Math.max(initialPoolSize, 20);
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(Math.max(effectivePoolSize, topK))
                .build();

        List<Document> rawCandidates = vectorStore.similaritySearch(searchRequest);
        log.info("Stage 1 retrieved {} candidate chunks", rawCandidates.size());

        // 2. STAGE 2: Rerank Candidates
        RerankResult rerankResult = rerankingService.rerank(query, rawCandidates, topK, strategy);
        List<Document> rerankedDocs = rerankResult.selectedDocuments();

        // 3. STAGE 3: Augment Prompt
        Set<String> uniqueSources = new LinkedHashSet<>();
        StringBuilder contextText = new StringBuilder();

        if (rerankedDocs.isEmpty()) {
            contextText.append("No directly matching diagnostic documentation chunks found in the database.\n");
        } else {
            for (int i = 0; i < rerankedDocs.size(); i++) {
                Document doc = rerankedDocs.get(i);
                String source = (String) doc.getMetadata().getOrDefault("source", "Diagnostic Manual");
                uniqueSources.add(source);

                contextText.append(String.format("--- CONTEXT CHUNK [%d] (Source: %s) ---\n%s\n\n",
                        i + 1, source, doc.getText().trim()));
            }
        }

        // System message supporting ingested documentation with explicit LLM knowledge fallback for follow-ups/concepts
        String systemInstruction = """
                You are a certified Mercedes-Benz automotive diagnostic technician and master diagnostic specialist.
                Answer the user's question adhering to the following protocol:
                
                Core Rules:
                1. PRIMARY INGESTED DOCUMENTATION:
                   - When the provided diagnostic documentation contains the answer, answer strictly using that documentation.
                   - If the context covers multiple vehicle models (e.g., C-Class and S-Class), clearly state the findings for EACH model.
                   - Be precise with fault codes (DTCs), multimeter voltages, pinouts, symptoms, and diagnostic test steps.
                   - Always cite which vehicle guide (e.g., C-Class or S-Class) the information comes from.
                
                2. FOLLOW-UP, CONCEPTUAL & GENERAL DEFINITION QUESTIONS (LLM KNOWLEDGE FALLBACK):
                   - If the question is a follow-up, conceptual definition, or broader automotive question (e.g., "What does cold charge air pipe mean?", component definitions, operational principles) that is NOT fully explained or defined in the provided context:
                   - You MUST provide an accurate, clear explanation using your general automotive engineering knowledge.
                   - CRITICAL MANDATE: You MUST explicitly inform the user that this explanation is NOT based on the fed/uploaded documentation, but is synthesized from general automotive engineering knowledge.
                   - Place this clear disclaimer at the top of your answer:
                     "> ℹ️ **Note**: *The provided workshop manuals do not contain a specific definition for this term. The following explanation is based on general automotive engineering knowledge (not from the uploaded diagnostic manuals):*"
                
                3. Keep answers technical, direct, structured, and concise.
                """;

        String userPromptText = String.format("DOCUMENTATION CONTEXT:\n%s\nUSER QUESTION:\n%s",
                contextText.toString().trim(), query.trim());

        Message systemMessage = new SystemMessage(systemInstruction);
        Message userMessage = new UserMessage(userPromptText);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        // 4. STAGE 4: Generate Answer
        log.info("Generating answer with reranked context using model: {}", modelName);
        String answer = chatModel.call(prompt).getResult().getOutput().getText();

        String promptPreview = userPromptText.length() > 300
                ? userPromptText.substring(0, 300) + "... [TRUNCATED]"
                : userPromptText;

        return new RagV2AnswerResponse(
                query,
                answer,
                modelName,
                new ArrayList<>(uniqueSources),
                rawCandidates.size(),
                rerankedDocs.size(),
                rerankResult.rerankedInfo(),
                promptPreview,
                strategy.name()
        );
    }
}
