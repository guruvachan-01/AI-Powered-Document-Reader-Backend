package com.docqa.service;

import com.docqa.model.Document;
import com.docqa.model.User;
import com.docqa.repository.DocumentRepository;
import com.docqa.repository.TimestampRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock TimestampRepository timestampRepository;
    @Mock PdfExtractionService pdfExtractionService;
    @Mock AiService aiService;

    @InjectMocks DocumentService documentService;

    private User testUser;
    private Document testDoc;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("testuser").build();
        testDoc = Document.builder()
                .id(1L).filename("test.pdf")
                .originalFilename("test.pdf")
                .fileType(Document.FileType.PDF)
                .fileSize(1024L)
                .user(testUser)
                .build();
    }

    @Test
    void getUserDocuments_ShouldReturnDocumentList() {
        when(documentRepository.findByUserOrderByUploadedAtDesc(testUser))
                .thenReturn(List.of(testDoc));

        var result = documentService.getUserDocuments(testUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFilename()).isEqualTo("test.pdf");
    }

    @Test
    void getDocumentById_ShouldReturnDocument_WhenFound() {
        when(documentRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testDoc));

        var result = documentService.getDocumentById(1L, testUser);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getDocumentById_ShouldThrow_WhenNotFound() {
        when(documentRepository.findByIdAndUser(99L, testUser))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocumentById(99L, testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void deleteDocument_ShouldDeleteFromRepo() {
        testDoc.setFilepath("/tmp/test.pdf");
        when(documentRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testDoc));

        documentService.deleteDocument(1L, testUser);

        verify(documentRepository).delete(testDoc);
    }

    @Test
    void getUserDocuments_ShouldReturnEmptyList_WhenNoDocuments() {
        when(documentRepository.findByUserOrderByUploadedAtDesc(testUser))
                .thenReturn(List.of());

        var result = documentService.getUserDocuments(testUser);

        assertThat(result).isEmpty();
    }
}
