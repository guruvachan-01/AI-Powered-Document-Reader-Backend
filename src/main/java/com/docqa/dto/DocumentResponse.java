package com.docqa.dto;

import com.docqa.model.Document;


import lombok.*;

import java.time.LocalDateTime;


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
