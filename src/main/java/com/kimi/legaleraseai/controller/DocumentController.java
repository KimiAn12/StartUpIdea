package com.kimi.legaleraseai.controller;

import com.kimi.legaleraseai.dto.MessageResponse;
import com.kimi.legaleraseai.entity.Document;
import com.kimi.legaleraseai.entity.User;
import com.kimi.legaleraseai.repository.DocumentRepository;
import com.kimi.legaleraseai.repository.UserRepository;
import com.kimi.legaleraseai.security.UserPrincipal;
import com.kimi.legaleraseai.service.DocumentProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentProcessingService documentProcessingService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file,
                                          Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Document document = documentProcessingService.processDocument(file, user);
            return ResponseEntity.ok(new DocumentResponse(document));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error uploading document: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> getUserDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Document> documents;

        if (search != null && !search.trim().isEmpty()) {
            documents = documentRepository.findByUserAndSearchTerm(user, search.trim(), pageable);
        } else {
            documents = documentRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        }

        Page<DocumentResponse> documentResponses = documents.map(DocumentResponse::new);
        return ResponseEntity.ok(documentResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Document> documentOpt = documentRepository.findByIdAndUser(id, user);
        if (documentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new DocumentResponse(documentOpt.get()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id, Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Document> documentOpt = documentRepository.findByIdAndUser(id, user);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            documentProcessingService.deleteDocument(documentOpt.get());
            return ResponseEntity.ok(new MessageResponse("Document deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error deleting document: " + e.getMessage()));
        }
    }

    // Inner class for response DTOs
    public static class DocumentResponse {
        private Long id;
        private String fileName;
        private String originalName;
        private Long fileSize;
        private String contentType;
        private String processingStatus;
        private String processingError;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
        private boolean hasExtractedText;

        public DocumentResponse(Document document) {
            this.id = document.getId();
            this.fileName = document.getFileName();
            this.originalName = document.getOriginalName();
            this.fileSize = document.getFileSize();
            this.contentType = document.getContentType();
            this.processingStatus = document.getProcessingStatus().name();
            this.processingError = document.getProcessingError();
            this.createdAt = document.getCreatedAt();
            this.updatedAt = document.getUpdatedAt();
            this.hasExtractedText = document.getExtractedText() != null && !document.getExtractedText().trim().isEmpty();
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }

        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }

        public String getProcessingStatus() { return processingStatus; }
        public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }

        public String getProcessingError() { return processingError; }
        public void setProcessingError(String processingError) { this.processingError = processingError; }

        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public boolean isHasExtractedText() { return hasExtractedText; }
        public void setHasExtractedText(boolean hasExtractedText) { this.hasExtractedText = hasExtractedText; }
    }
}
