package com.kimi.legaleraseai.service;

import com.kimi.legaleraseai.entity.Document;
import com.kimi.legaleraseai.entity.User;
import com.kimi.legaleraseai.repository.DocumentRepository;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);
    private static final String UPLOAD_DIR = "uploads/documents";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    @Autowired
    private DocumentRepository documentRepository;

    private final Tika tika = new Tika();

    public Document processDocument(MultipartFile file, User user) throws IOException {
        logger.info("=== Starting document processing ===");
        logger.info("File: {}, Size: {}, Type: {}", file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        validateFile(file);
        logger.info("File validation passed");
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created upload directory: {}", uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFilename);
        logger.info("Generated filename: {} -> {}", originalFilename, uniqueFilename);

        // Save file to disk
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("File saved to disk: {}", filePath);

        // Create document entity
        Document document = new Document(
                uniqueFilename,
                originalFilename,
                file.getContentType(),
                file.getSize(),
                user
        );
        document.setFilePath(filePath.toString());
        document.setProcessingStatus(Document.ProcessingStatus.PROCESSING);

        // Save document to database
        document = documentRepository.save(document);
        logger.info("Document saved to database with ID: {}", document.getId());

        // Extract text asynchronously (in real implementation, use @Async)
        logger.info("Starting text extraction...");
        try {
            String extractedText = extractTextFromFile(filePath.toFile());
            logger.info("Text extraction successful, length: {} characters", extractedText.length());
            document.setExtractedText(extractedText);
            document.setProcessingStatus(Document.ProcessingStatus.COMPLETED);
            logger.info("Document processing COMPLETED: {}", originalFilename);
        } catch (Exception e) {
            logger.error("Text extraction FAILED for document: {}", originalFilename, e);
            document.setProcessingStatus(Document.ProcessingStatus.FAILED);
            document.setProcessingError(e.getMessage());
        }

        Document finalDocument = documentRepository.save(document);
        logger.info("Final document status: {}", finalDocument.getProcessingStatus());
        return finalDocument;
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum allowed size of 50MB");
        }

        String contentType = file.getContentType();
        if (!isValidContentType(contentType)) {
            throw new IOException("Invalid file type. Only PDF and Word documents are allowed.");
        }
    }

    private boolean isValidContentType(String contentType) {
        return contentType != null && (
                contentType.equals("application/pdf") ||
                contentType.equals("application/msword") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private String extractTextFromFile(File file) throws IOException, TikaException {
        logger.info("Extracting text from file: {}", file.getName());
        logger.info("File exists: {}, File size: {} bytes", file.exists(), file.length());
        
        try {
            logger.info("Starting Tika text extraction...");
            String text = tika.parseToString(file);
            logger.info("Tika extraction completed, raw text length: {}", text != null ? text.length() : 0);
            
            if (text != null) {
                String trimmedText = text.trim();
                logger.info("Text trimmed, final length: {}", trimmedText.length());
                return trimmedText;
            } else {
                logger.warn("Tika returned null text");
                return "";
            }
        } catch (Exception e) {
            logger.error("Error parsing file with Tika: {}", file.getName(), e);
            throw new TikaException("Failed to extract text from file: " + e.getMessage());
        }
    }

    public void deleteDocument(Document document) throws IOException {
        // Delete file from disk
        if (document.getFilePath() != null) {
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted file: {}", document.getFilePath());
            }
        }

        // Delete from database
        documentRepository.delete(document);
        logger.info("Deleted document from database: {}", document.getOriginalName());
    }
}
