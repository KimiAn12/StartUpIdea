package com.kimi.legaleraseai.controller;

import com.kimi.legaleraseai.dto.MessageResponse;
import com.kimi.legaleraseai.entity.*;
import com.kimi.legaleraseai.repository.DocumentAnalysisRepository;
import com.kimi.legaleraseai.repository.DocumentRepository;
import com.kimi.legaleraseai.repository.ExtractedClauseRepository;
import com.kimi.legaleraseai.repository.UserRepository;
import com.kimi.legaleraseai.security.UserPrincipal;
import com.kimi.legaleraseai.service.GeminiAiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private GeminiAiService geminiAiService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentAnalysisRepository analysisRepository;

    @Autowired
    private ExtractedClauseRepository clauseRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/documents/{documentId}/summarize")
    public ResponseEntity<?> summarizeDocument(@PathVariable Long documentId, 
                                             Authentication authentication) {
        try {
            Document document = getDocumentForUser(documentId, authentication);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            DocumentAnalysis analysis = geminiAiService.summarizeDocument(document);
            return ResponseEntity.ok(new AnalysisResponse(analysis));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error summarizing document: " + e.getMessage()));
        }
    }

    @PostMapping("/documents/{documentId}/extract-clauses")
    public ResponseEntity<?> extractClauses(@PathVariable Long documentId,
                                          Authentication authentication) {
        try {
            Document document = getDocumentForUser(documentId, authentication);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            List<ExtractedClause> clauses = geminiAiService.extractClauses(document);
            List<ClauseResponse> clauseResponses = clauses.stream()
                    .map(ClauseResponse::new)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(clauseResponses);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error extracting clauses: " + e.getMessage()));
        }
    }

    @PostMapping("/documents/{documentId}/question")
    public ResponseEntity<?> askQuestion(@PathVariable Long documentId,
                                       @Valid @RequestBody QuestionRequest request,
                                       Authentication authentication) {
        try {
            Document document = getDocumentForUser(documentId, authentication);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            DocumentAnalysis analysis = geminiAiService.answerQuestion(document, request.getQuestion());
            return ResponseEntity.ok(new AnalysisResponse(analysis));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error answering question: " + e.getMessage()));
        }
    }

    @PostMapping("/templates/generate")
    public ResponseEntity<?> generateTemplate(@Valid @RequestBody TemplateRequest request,
                                            Authentication authentication) {
        try {
            DocumentAnalysis analysis = geminiAiService.generateTemplate(
                    request.getTemplateType(), 
                    request.getRequirements()
            );
            return ResponseEntity.ok(new AnalysisResponse(analysis));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error generating template: " + e.getMessage()));
        }
    }

    @GetMapping("/documents/{documentId}/analyses")
    public ResponseEntity<?> getDocumentAnalyses(@PathVariable Long documentId,
                                                Authentication authentication) {
        Document document = getDocumentForUser(documentId, authentication);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        List<DocumentAnalysis> analyses = analysisRepository.findByDocumentOrderByCreatedAtDesc(document);
        List<AnalysisResponse> analysisResponses = analyses.stream()
                .map(AnalysisResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(analysisResponses);
    }

    @GetMapping("/documents/{documentId}/clauses")
    public ResponseEntity<?> getDocumentClauses(@PathVariable Long documentId,
                                              Authentication authentication) {
        Document document = getDocumentForUser(documentId, authentication);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        List<ExtractedClause> clauses = clauseRepository
                .findByDocumentOrderByImportanceLevelDescCreatedAtDesc(document);
        List<ClauseResponse> clauseResponses = clauses.stream()
                .map(ClauseResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(clauseResponses);
    }

    private Document getDocumentForUser(Long documentId, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Document> documentOpt = documentRepository.findByIdAndUser(documentId, user);
        return documentOpt.orElse(null);
    }

    // Request DTOs
    public static class QuestionRequest {
        private String question;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
    }

    public static class TemplateRequest {
        private String templateType;
        private String requirements;

        public String getTemplateType() { return templateType; }
        public void setTemplateType(String templateType) { this.templateType = templateType; }

        public String getRequirements() { return requirements; }
        public void setRequirements(String requirements) { this.requirements = requirements; }
    }

    // Response DTOs
    public static class AnalysisResponse {
        private Long id;
        private String analysisType;
        private String result;
        private String prompt;
        private Double confidenceScore;
        private String status;
        private String errorMessage;
        private LocalDateTime createdAt;

        public AnalysisResponse(DocumentAnalysis analysis) {
            this.id = analysis.getId();
            this.analysisType = analysis.getAnalysisType().name();
            this.result = analysis.getResult();
            this.prompt = analysis.getPrompt();
            this.confidenceScore = analysis.getConfidenceScore();
            this.status = analysis.getStatus().name();
            this.errorMessage = analysis.getErrorMessage();
            this.createdAt = analysis.getCreatedAt();
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getAnalysisType() { return analysisType; }
        public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }

        public Double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class ClauseResponse {
        private Long id;
        private String clauseType;
        private String clauseText;
        private String plainEnglishExplanation;
        private String importanceLevel;
        private Double confidenceScore;
        private LocalDateTime createdAt;

        public ClauseResponse(ExtractedClause clause) {
            this.id = clause.getId();
            this.clauseType = clause.getClauseType();
            this.clauseText = clause.getClauseText();
            this.plainEnglishExplanation = clause.getPlainEnglishExplanation();
            this.importanceLevel = clause.getImportanceLevel().name();
            this.confidenceScore = clause.getConfidenceScore();
            this.createdAt = clause.getCreatedAt();
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getClauseType() { return clauseType; }
        public void setClauseType(String clauseType) { this.clauseType = clauseType; }

        public String getClauseText() { return clauseText; }
        public void setClauseText(String clauseText) { this.clauseText = clauseText; }

        public String getPlainEnglishExplanation() { return plainEnglishExplanation; }
        public void setPlainEnglishExplanation(String plainEnglishExplanation) { 
            this.plainEnglishExplanation = plainEnglishExplanation; 
        }

        public String getImportanceLevel() { return importanceLevel; }
        public void setImportanceLevel(String importanceLevel) { this.importanceLevel = importanceLevel; }

        public Double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
