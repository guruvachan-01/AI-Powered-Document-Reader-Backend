package com.docqa.dto;

import com.docqa.model.Document;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentResponse {
    private Long id;
    private String filename;
    private String originalFilename;
    private Document.FileType fileType;
    private Long fileSize;
    private String summary;
    private LocalDateTime uploadedAt;
    private int messageCount;
}
