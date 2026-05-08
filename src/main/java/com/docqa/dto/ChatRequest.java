package com.docqa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ChatRequest {
    @NotBlank
    private String question;
    private Long documentId;
}
