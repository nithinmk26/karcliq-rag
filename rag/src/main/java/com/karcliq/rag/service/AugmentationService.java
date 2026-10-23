package com.karcliq.rag.service;

import com.karcliq.rag.model.RagAnswerResponse;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AugmentationService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    @Value("${spring.ai.openai.model:gpt-4o-mini}")
    private String modelName;

    public RagAnswerResponse ask(String query, int topK, Double similarityThreshold) {
        log.info("RAG Augmentation Pipeline: query='{}', topK={}", query, topK);

        SearchRequest.Builder requestBuilder = SearchRequest.builder()
                .query(query)
                .topK(Math.max(topK, 5));

        if (similarityThreshold != null && similarityThreshold > 0.0) {
            requestBuilder.similarityThreshold(similarityThreshold);
        }

        List<Document> matchedDocs = vectorStore.similaritySearch(requestBuilder.build());
        log.info("Retrieved {} context chunks for V1 augmentation", matchedDocs.size());

        Set<String> uniqueSources = new LinkedHashSet<>();
        StringBuilder contextText = new StringBuilder();

        if (matchedDocs.isEmpty()) {
            contextText.append("No directly matching diagnostic documentation chunks found in the database.\n");
        } else {
            for (int i = 0; i < matchedDocs.size(); i++) {
                Document doc = matchedDocs.get(i);
                String source = (String) doc.getMetadata().getOrDefault("source", "Diagnostic Manual");
                uniqueSources.add(source);
                log.info("\n--- [V1 Context Chunk #{}/{}] ---\nID: {}\nSource: {}\nContent:\n{}\n----------------------------------------",
                        i + 1, matchedDocs.size(), doc.getId(), source, doc.getText() != null ? doc.getText().trim() : "");

                contextText.append(String.format("--- CONTEXT CHUNK [%d] (Source: %s) ---\n%s\n\n",
                        i + 1, source, doc.getText().trim()));
            }
        }

        String systemInstruction = """
                You are a certified Mercedes-Benz automotive diagnostic technician and master diagnostic specialist.
                Answer the user's question adhering to the following protocol:
                
                Core Rules:
                1. PRIMARY INGESTED DOCUMENTATION:
                   - When the provided diagnostic documentation contains the answer, answer strictly using that documentation.
                   - Be precise with fault codes (DTCs), multimeter voltages, pinouts, symptoms, and diagnostic test steps.
                   - Always cite which vehicle guide (e.g., C-Class or S-Class) the information comes from.
                
                2. FOLLOW-UP, CONCEPTUAL & GENERAL DEFINITION QUESTIONS (LLM KNOWLEDGE FALLBACK):
                   - If the question is a follow-up, conceptual definition, or broader automotive question (e.g., "What does cold charge air pipe mean?", component definitions, operational principles) that is NOT fully explained or defined in the provided context:
                   - You MUST provide an accurate, clear explanation using your general automotive engineering knowledge.
                   - CRITICAL MANDATE: You MUST explicitly inform the user that this explanation is NOT based on the fed/uploaded documentation, but is synthesized from general automotive engineering knowledge.
                   - Place this clear disclaimer at the top of your answer:
                     "> **Note**: *The provided workshop manuals do not contain a specific definition for this term. The following explanation is based on general automotive engineering knowledge (not from the uploaded diagnostic manuals):*"
                
                3. Keep answers technical, direct, structured, and concise.
                """;

        String userPromptText = String.format("DOCUMENTATION CONTEXT:\n%s\nUSER QUESTION:\n%s",
                contextText.toString().trim(), query.trim());

        Message systemMessage = new SystemMessage(systemInstruction);
        Message userMessage = new UserMessage(userPromptText);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        log.info("Sending augmented prompt to model: {}", modelName);
        String answer = chatModel.call(prompt).getResult().getOutput().getText();

        String promptPreview = userPromptText.length() > 300
                ? userPromptText.substring(0, 300) + "... [TRUNCATED]"
                : userPromptText;

        return new RagAnswerResponse(
                query,
                answer,
                modelName,
                new ArrayList<>(uniqueSources),
                matchedDocs.size(),
                promptPreview
        );
    }
}
