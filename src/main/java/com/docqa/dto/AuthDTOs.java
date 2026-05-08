package com.docqa.dto;

import com.docqa.model.Document;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

// ===== Auth DTOs =====
public class AuthDTOs {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank @Size(min = 3, max = 50)
        private String username;
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 6)
        private String password;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank private String email;
        @NotBlank private String password;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String token;
        private String username;
        private String email;
        private String role;
    }
}

// ===== Document DTOs =====
class DocumentDTOs {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DocumentResponse {
        private Long id;
        private String filename;
        private String originalFilename;
        private Document.FileType fileType;
        private Long fileSize;
        private String summary;
        private LocalDateTime uploadedAt;
    }
}

// ===== Chat DTOs =====
class ChatDTOs {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ChatRequest {
        @NotBlank
        private String question;
        private Long documentId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChatResponse {
        private String answer;
        private Double timestampRef;
        private List<TimestampDto> relevantTimestamps;
        private Long messageId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TimestampDto {
        private Double startTime;
        private Double endTime;
        private String text;
        private String topic;
    }
}
