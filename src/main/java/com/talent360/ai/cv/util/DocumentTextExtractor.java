package com.talent360.ai.cv.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class DocumentTextExtractor {

    public String extractText(Path filePath, String fileType) {
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }

        String fileName = filePath.getFileName().toString().toLowerCase();

        try {
            if (fileName.endsWith(".pdf") || (fileType != null && fileType.contains("pdf"))) {
                return extractTextFromPdf(filePath.toFile());
            } else if (fileName.endsWith(".docx") || (fileType != null && fileType.contains("word"))) {
                return extractTextFromDocx(filePath);
            } else {
                return Files.readString(filePath);
            }
        } catch (Exception ex) {
            log.error("Failed to extract text from document {}: {}", filePath, ex.getMessage());
            throw new RuntimeException("Could not extract readable text from document: " + ex.getMessage(), ex);
        }
    }

    private String extractTextFromPdf(File file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(Path path) throws Exception {
        try (InputStream is = Files.newInputStream(path);
             XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
