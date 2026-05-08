package com.docqa.service;

import com.docqa.dto.ChatRequest;
import com.docqa.dto.ChatResponse;
import com.docqa.model.ChatMessage;
import com.docqa.model.Document;
import com.docqa.model.Timestamp;
import com.docqa.model.User;
import com.docqa.repository.ChatMessageRepository;
import com.docqa.repository.TimestampRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatMessageRepository chatMessageRepository;
    @Mock TimestampRepository timestampRepository;
    @Mock DocumentService documentService;
    @Mock AiService aiService;

    @InjectMocks ChatService chatService;

    private User testUser;
    private Document testDoc;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("testuser").build();
        testDoc = Document.builder()
                .id(1L)
                .fileType(Document.FileType.PDF)
                .content("This is sample document content about Java Spring Boot.")
                .user(testUser)
                .build();
    }

    @Test
    void chat_ShouldReturnAiResponse_WhenDocumentHasContent() {
        ChatRequest request = new ChatRequest("What is this about?", 1L);
        ChatResponse aiResponse = ChatResponse.builder()
                .answer("This document is about Java Spring Boot.")
                .build();

        when(documentService.getDocumentById(1L, testUser)).thenReturn(testDoc);
        when(aiService.askQuestion(anyString(), anyString(), anyList(), anyString()))
                .thenReturn(aiResponse);

        ChatMessage savedMsg = ChatMessage.builder().id(5L).build();
        when(chatMessageRepository.save(any())).thenReturn(
                ChatMessage.builder().build(),
                savedMsg
        );

        ChatResponse result = chatService.chat(request, testUser);

        assertThat(result.getAnswer()).isEqualTo("This document is about Java Spring Boot.");
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
        verify(aiService).askQuestion(anyString(), anyString(), anyList(), anyString());
    }

    @Test
    void chat_ShouldReturnProcessingMessage_WhenContentIsEmpty() {
        testDoc.setContent("");
        ChatRequest request = new ChatRequest("What is this?", 1L);
        when(documentService.getDocumentById(1L, testUser)).thenReturn(testDoc);

        ChatResponse result = chatService.chat(request, testUser);

        assertThat(result.getAnswer()).contains("still being processed");
        verify(aiService, never()).askQuestion(anyString(), anyString(), anyList(), anyString());
    }

    @Test
    void getChatHistory_ShouldReturnMessageList() {
        ChatMessage msg = ChatMessage.builder()
                .id(1L).content("Hello").role(ChatMessage.MessageRole.USER)
                .build();
        when(documentService.getDocumentById(1L, testUser)).thenReturn(testDoc);
        when(chatMessageRepository.findByDocumentOrderByCreatedAtAsc(testDoc))
                .thenReturn(List.of(msg));

        List<ChatMessage> result = chatService.getChatHistory(1L, testUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Hello");
    }

    @Test
    void clearChatHistory_ShouldDeleteMessages() {
        when(documentService.getDocumentById(1L, testUser)).thenReturn(testDoc);

        chatService.clearChatHistory(1L, testUser);

        verify(chatMessageRepository).deleteByDocument(testDoc);
    }

    @Test
    void chat_ShouldFetchTimestamps_ForMediaDocuments() {
        testDoc.setFileType(Document.FileType.AUDIO);
        ChatRequest request = new ChatRequest("What was discussed at minute 5?", 1L);

        Timestamp ts = new Timestamp(1L, 60.0, 120.0, "Discussion", "main topic", testDoc);
        when(documentService.getDocumentById(1L, testUser)).thenReturn(testDoc);
        when(timestampRepository.findByDocumentOrderByStartTimeAsc(testDoc)).thenReturn(List.of(ts));
        when(aiService.askQuestion(anyString(), anyString(), anyList(), anyString()))
                .thenReturn(ChatResponse.builder().answer("At minute 5...").build());
        when(chatMessageRepository.save(any())).thenReturn(ChatMessage.builder().id(1L).build());

        chatService.chat(request, testUser);

        verify(timestampRepository).findByDocumentOrderByStartTimeAsc(testDoc);
    }
}
