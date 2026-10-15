package com.karcliq.rag.service;

import com.karcliq.rag.model.RagAnswerResponse;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AugmentationServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private AugmentationService augmentationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(augmentationService, "modelName", "gpt-4o-mini");
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

    // 1. Positive Test: Context chunks found, answer generated
    @Test
    void testAsk_Positive() {
        Document doc = new Document("1", "Check MAF sensor wiring.", Map.of("source", "C_Class_P0101.pdf"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));
        mockChatResponse("Inspect the MAF sensor wiring for damage.");

        RagAnswerResponse response = augmentationService.ask("What is P0101?", 4, null);

        assertNotNull(response);
        assertEquals("Inspect the MAF sensor wiring for damage.", response.answer());
        assertEquals(1, response.chunksUsed());
        assertFalse(response.sources().isEmpty());
    }

    // 2. Negative Test: No context chunks found, uses fallback
    @Test
    void testAsk_Negative() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());
        mockChatResponse("General automotive explanation.");

        RagAnswerResponse response = augmentationService.ask("What is turbo lag?", 4, null);

        assertNotNull(response);
        assertEquals("General automotive explanation.", response.answer());
        assertEquals(0, response.chunksUsed());
        assertTrue(response.sources().isEmpty());
    }
}
