package com.docqa.controller;

import com.docqa.dto.ChatRequest;

import com.docqa.dto.ChatResponse;

import com.docqa.model.User;
import com.docqa.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI-powered document Q&A")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Ask a question about a document")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.chat(request, user));
    }

    @GetMapping("/history/{documentId}")
    @Operation(summary = "Get chat history for a document")
    public ResponseEntity<?> getHistory(
            @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getChatHistory(documentId, user));
    }

    @DeleteMapping("/history/{documentId}")
    @Operation(summary = "Clear chat history for a document")
    public ResponseEntity<Void> clearHistory(
            @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {
        chatService.clearChatHistory(documentId, user);
        return ResponseEntity.noContent().build();
    }
}
