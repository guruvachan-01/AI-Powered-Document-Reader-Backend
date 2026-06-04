package com.docqa.service;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;


import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PdfExtractionServiceTest {

    @InjectMocks PdfExtractionService pdfExtractionService;

    @TempDir Path tempDir;

    @Test
    void extractText_ShouldThrow_WhenFileNotFound() {
        assertThatThrownBy(() -> pdfExtractionService.extractText("/nonexistent/file.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process PDF");
    }

    @Test
    void getPageCount_ShouldReturnZero_WhenFileNotFound() {
        int count = pdfExtractionService.getPageCount("/nonexistent/file.pdf");
        assertThat(count).isEqualTo(0);
    }
}
