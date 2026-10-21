package com.karcliq.rag.service.v2;

import com.karcliq.rag.model.v2.RagV2AnswerResponse;
import com.karcliq.rag.model.v2.RagV2AnswerResponse.RerankedChunkInfo;
import com.karcliq.rag.service.v2.RerankingService.RerankResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AugmentationServiceV2Test {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatModel chatModel;

    @Mock
    private RerankingService rerankingService;

    @InjectMocks
    private AugmentationServiceV2 augmentationServiceV2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(augmentationServiceV2, "modelName", "gpt-4o-mini");
    }

    private void mockChatResponse(String responseText) {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage assistantMessage = mock(AssistantMessage.class);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(assistantMessage);
        when(assistantMessage.getText()).thenReturn(responseText);
    }

    // 1. Positive Test: Finds candidates, reranks with CROSS_ENCODER, and returns V2 answer
    @Test
    void testAskWithRerank_Positive() {
        Document doc = new Document("1", "C 300 P0301 misfire repair guide", Map.of("source", "C_Class_P0301.pdf"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        RerankedChunkInfo info = new RerankedChunkInfo("1", "C_Class_P0301.pdf", 1, 1, 0.8, 0.9, "snippet");
        when(rerankingService.rerank(eq("P0301"), any(), eq(2), any())).thenReturn(new RerankResult(List.of(doc), List.of(info)));

        mockChatResponse("C 300 Injector resistance is 1.8 ohms.");

        RagV2AnswerResponse response = augmentationServiceV2.askWithRerank("P0301", 2, 10, com.karcliq.rag.model.v2.RerankStrategy.CROSS_ENCODER);

        assertNotNull(response);
        assertEquals("C 300 Injector resistance is 1.8 ohms.", response.answer());
        assertEquals(1, response.topKSelected());
        assertEquals("CROSS_ENCODER", response.rerankStrategy());
    }

    // 1b. Positive Test: Reranks with HYBRID strategy
    @Test
    void testAskWithRerank_HybridStrategy() {
        Document doc = new Document("1", "C 300 P0301 misfire repair guide", Map.of("source", "C_Class_P0301.pdf"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        RerankedChunkInfo info = new RerankedChunkInfo("1", "C_Class_P0301.pdf", 1, 1, 0.8, 0.85, "snippet");
        when(rerankingService.rerank(eq("P0301"), any(), eq(2), eq(com.karcliq.rag.model.v2.RerankStrategy.HYBRID))).thenReturn(new RerankResult(List.of(doc), List.of(info)));

        mockChatResponse("C 300 Hybrid answer.");

        RagV2AnswerResponse response = augmentationServiceV2.askWithRerank("P0301", 2, 10, com.karcliq.rag.model.v2.RerankStrategy.HYBRID);

        assertNotNull(response);
        assertEquals("C 300 Hybrid answer.", response.answer());
        assertEquals("HYBRID", response.rerankStrategy());
    }

    // 2. Negative Test: No candidates found, handles empty state
    @Test
    void testAskWithRerank_Negative() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());
        when(rerankingService.rerank(eq("unknown"), any(), eq(2), any())).thenReturn(new RerankResult(Collections.emptyList(), Collections.emptyList()));
        mockChatResponse("General automotive knowledge fallback.");

        RagV2AnswerResponse response = augmentationServiceV2.askWithRerank("unknown", 2, 10);

        assertNotNull(response);
        assertEquals("General automotive knowledge fallback.", response.answer());
        assertEquals(0, response.topKSelected());
        assertTrue(response.sources().isEmpty());
    }
}
