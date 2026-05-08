package com.docqa.service;

import com.docqa.dto.ChatHistoryResponse;
import com.docqa.dto.ChatRequest;
import com.docqa.dto.ChatResponse;
import com.docqa.model.ChatMessage;
import com.docqa.model.Document;
import com.docqa.model.Timestamp;
import com.docqa.model.User;
import com.docqa.repository.ChatMessageRepository;
import com.docqa.repository.TimestampRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final TimestampRepository timestampRepository;
    private final DocumentService documentService;
    private final AiService aiService;

    @Transactional
    public ChatResponse chat(ChatRequest request, User user) {

        // Get document
        Document document = documentService.getDocumentById(
                request.getDocumentId(),
                user
        );

        // Check processing
        if (document.getContent() == null || document.getContent().isBlank()) {
            return ChatResponse.builder()
                    .answer("Document is still being processed. Please wait a moment and try again.")
                    .build();
        }

        // Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .content(request.getQuestion())
                .role(ChatMessage.MessageRole.USER)
                .document(document)
                .user(user)
                .build();

        chatMessageRepository.save(userMsg);

        // Fetch previous chat history
        List<ChatMessage> history =
                chatMessageRepository.findByDocumentOrderByCreatedAtAsc(document);

        // Build conversation context
        String conversationHistory = history.stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        // Fetch timestamps if media
        List<Timestamp> timestamps = List.of();

        if (document.getFileType() != Document.FileType.PDF) {
            timestamps = timestampRepository
                    .findByDocumentOrderByStartTimeAsc(document);
        }

        // Create enhanced prompt
        String enhancedQuestion = """
                User Name: %s

                Previous Conversation:
                %s

                Current Question:
                %s
                """.formatted(
                user.getUsername(),
                conversationHistory,
                request.getQuestion()
        );

        // Ask AI
        ChatResponse response = aiService.askQuestion(
                enhancedQuestion,
                document.getContent(),
                timestamps,
                document.getFileType().name()
        );

        // Save assistant response
        ChatMessage assistantMsg = ChatMessage.builder()
                .content(response.getAnswer())
                .role(ChatMessage.MessageRole.ASSISTANT)
                .document(document)
                .user(user)
                .timestampRef(response.getTimestampRef())
                .build();

        ChatMessage saved = chatMessageRepository.save(assistantMsg);

        response.setMessageId(saved.getId());

        return response;
    }

    public List<ChatHistoryResponse> getChatHistory(Long documentId, User user) {

        Document document =
                documentService.getDocumentById(documentId, user);

        return chatMessageRepository
                .findByDocumentOrderByCreatedAtAsc(document)
                .stream()
                .map(msg -> ChatHistoryResponse.builder()
                        .id(msg.getId())
                        .content(msg.getContent())
                        .role(msg.getRole().name())
                        .timestampRef(
                        	    msg.getTimestampRef() != null
                        	        ? msg.getTimestampRef().toString()
                        	        : null
                        	)
                        .build())
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void clearChatHistory(Long documentId, User user) {

        Document document =
                documentService.getDocumentById(documentId, user);

        chatMessageRepository.deleteByDocument(document);
    }
}