package com.docqa.controller;

import com.docqa.dto.DocumentResponse;
import com.docqa.model.User;
import com.docqa.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Upload and manage documents")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PDF, audio, or video file")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ResponseEntity.ok(documentService.uploadDocument(file, user));
    }

    @GetMapping
    @Operation(summary = "Get all documents for current user")
    public ResponseEntity<List<DocumentResponse>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(documentService.getUserDocuments(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific document")
    public ResponseEntity<DocumentResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        var doc = documentService.getDocumentById(id, user);
        return ResponseEntity.ok(DocumentResponse.builder()
                .id(doc.getId())
                .filename(doc.getFilename())
                .originalFilename(doc.getOriginalFilename())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .summary(doc.getSummary())
                .uploadedAt(doc.getUploadedAt())
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        documentService.deleteDocument(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/files/{filename:.+}")
    @Operation(summary = "Serve file content")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            var uploadDir = System.getProperty("app.upload.dir", "./uploads");
            var path = Paths.get(uploadDir).resolve(filename);
            Resource resource = new FileSystemResource(path.toFile());

            if (!resource.exists()) return ResponseEntity.notFound().build();

            String contentType = Files.probeContentType(path);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
