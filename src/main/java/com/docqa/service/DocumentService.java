package com.docqa.service;

import com.docqa.dto.DocumentResponse;
import com.docqa.model.Document;
import com.docqa.model.User;
import com.docqa.repository.DocumentRepository;
import com.docqa.repository.TimestampRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final TimestampRepository timestampRepository;
    private final PdfExtractionService pdfExtractionService;
    private final AiService aiService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, User user) throws IOException {
        // Determine file type
        String contentType = file.getContentType();
        Document.FileType fileType = determineFileType(contentType, file.getOriginalFilename());

        // Save file to disk
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(filename);

        file.transferTo(filePath.toFile());

        // Create document entity
        Document document = Document.builder()
                .filename(filename)
                .originalFilename(file.getOriginalFilename())
                .filepath(filePath.toString())
                .fileType(fileType)
                .fileSize(file.getSize())
                .user(user)
                .build();

        document = documentRepository.save(document);

        // Extract content asynchronously
        processDocumentAsync(document);

        return toResponse(document);
    }

    @Async
    public void processDocumentAsync(Document document) {
        try {
            String content = "";

            if (document.getFileType() == Document.FileType.PDF) {
                content = pdfExtractionService.extractText(document.getFilepath());
            } else if (document.getFileType() == Document.FileType.AUDIO ||
                       document.getFileType() == Document.FileType.VIDEO) {
                // Whisper transcription would be called here via OpenAI API
                content = transcribeMediaFile(document.getFilepath());
            }

            document.setContent(content);

            // Generate summary
            if (!content.isBlank()) {
                String summary = aiService.summarizeContent(content, document.getFileType().name());
                document.setSummary(summary);

                // Extract timestamps for audio/video
                if (document.getFileType() != Document.FileType.PDF) {
                    var timestamps = aiService.extractTimestamps(content);
                    timestamps.forEach(t -> t.setDocument(document));
                    timestampRepository.saveAll(timestamps);
                }
            }

            documentRepository.save(document);
            log.info("Document processed successfully: {}", document.getId());
        } catch (Exception e) {
            log.error("Failed to process document: {}", document.getId(), e);
        }
    }

    private String transcribeMediaFile(String filePath) {
        // In production, this calls OpenAI Whisper API
        // Example: https://api.openai.com/v1/audio/transcriptions
        // For now, return placeholder - implement with RestTemplate/WebClient
        log.info("Transcribing media file: {}", filePath);
        return "Audio transcription would appear here. Integrate OpenAI Whisper API for real transcription.";
    }

    public List<DocumentResponse> getUserDocuments(User user) {
        return documentRepository.findByUserOrderByUploadedAtDesc(user)
                .stream().map(this::toResponse).toList();
    }

    public Document getDocumentById(Long id, User user) {
        return documentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    @Transactional
    public void deleteDocument(Long id, User user) {
        Document doc = getDocumentById(id, user);
        // Delete physical file
        try { Files.deleteIfExists(Paths.get(doc.getFilepath())); } catch (IOException ignored) {}
        documentRepository.delete(doc);
    }

    private Document.FileType determineFileType(String contentType, String filename) {
        if (contentType == null) contentType = "";
        if (filename == null) filename = "";
        String lower = filename.toLowerCase();

        if (contentType.contains("pdf") || lower.endsWith(".pdf")) return Document.FileType.PDF;
        if (contentType.contains("audio") || lower.matches(".*\\.(mp3|wav|ogg|m4a|flac)"))
            return Document.FileType.AUDIO;
        if (contentType.contains("video") || lower.matches(".*\\.(mp4|avi|mov|mkv|webm)"))
            return Document.FileType.VIDEO;

        return Document.FileType.PDF; // default
    }

    private DocumentResponse toResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .filename(doc.getFilename())
                .originalFilename(doc.getOriginalFilename())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .summary(doc.getSummary())
                .uploadedAt(doc.getUploadedAt())
                .messageCount(doc.getMessages() != null ? doc.getMessages().size() : 0)
                .build();
    }
}
