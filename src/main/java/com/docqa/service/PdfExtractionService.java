package com.docqa.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class PdfExtractionService {

    public String extractText(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("Extracted {} characters from PDF: {}", text.length(), filePath);
            return text;
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", filePath, e);
            throw new RuntimeException("Failed to process PDF: " + e.getMessage());
        }
    }

    public int getPageCount(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            return 0;
        }
    }
}
