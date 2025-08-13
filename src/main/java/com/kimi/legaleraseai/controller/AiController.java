package com.kimi.legaleraseai.controller;

// Import statements for required classes and dependencies
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

/**
 * REST Controller for AI-powered document analysis operations
 * 
 * This controller handles all AI-related endpoints including:
 * - Document summarization
 * - Clause extraction
 * - Question answering
 * - Template generation
 * - Retrieving analysis results
 * 
 * @RestController: Indicates this is a REST controller that returns data (not views)
 * @RequestMapping("/api/ai"): Base path for all endpoints in this controller
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    // Dependency injection using @Autowired annotation
    // Spring will automatically provide instances of these services/repositories
    @Autowired
    private GeminiAiService geminiAiService;  // Service for AI operations using Google's Gemini

    // Handles document CRUD operations
    @Autowired
    private DocumentRepository documentRepository;

    // Manages document analysis results and history 
    @Autowired
    private DocumentAnalysisRepository analysisRepository; 

    // Handles extracted legal clauses
    @Autowired
    private ExtractedClauseRepository clauseRepository;  

    // Manages user data
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/test-db")
    public ResponseEntity<?> testDatabase() {
        try {
            // Test database connection by counting users
            long userCount = userRepository.count();
            return ResponseEntity.ok(new MessageResponse("Database connection successful. User count: " + userCount));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new MessageResponse("Database connection failed: " + e.getMessage()));
        }
    }

    @GetMapping("/test-db-no-auth")
    public ResponseEntity<?> testDatabaseNoAuth() {
        try {
            // Test database connection by counting users (no authentication required)
            long userCount = userRepository.count();
            return ResponseEntity.ok(new MessageResponse("Database connection successful (no auth). User count: " + userCount));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new MessageResponse("Database connection failed: " + e.getMessage()));
        }
    }

    @GetMapping("/test-auth")
    public ResponseEntity<?> testAuthentication(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponse("No authentication found"));
            }
            
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(new MessageResponse("Authentication successful for user: " + userPrincipal.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new MessageResponse("Authentication test failed: " + e.getMessage()));
        }
    }

    @PostMapping("/documents/{documentId}/summarize")
    public ResponseEntity<?> summarizeDocument(@PathVariable Long documentId, 
                                             Authentication authentication) {
        try {
            // Get document and verify user has access to it
            Document document = getDocumentForUser(documentId, authentication);
            if (document == null) {
                return ResponseEntity.notFound().build();  // Return 404 if document not found
            }

            // Use AI service to summarize the document
            DocumentAnalysis analysis = geminiAiService.summarizeDocument(document);
            return ResponseEntity.ok(new AnalysisResponse(analysis));  // Return 200 with summary
        } catch (Exception e) {
            // Return 400 with error message if something goes wrong
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error summarizing document: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to extract legal clauses from a document using AI
     * 
     * @PostMapping: HTTP POST request to "/documents/{documentId}/extract-clauses"
     * @param documentId: ID of the document to analyze
     * @param authentication: User authentication info
     * @return ResponseEntity: List of extracted clauses or error message
     */
    @PostMapping("/documents/{documentId}/extract-clauses")
    public ResponseEntity<?> extractClauses(@PathVariable Long documentId,
                                          Authentication authentication) {
        try {
            // Verify document access
            Document document = getDocumentForUser(documentId, authentication);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            // Extract clauses using AI service
            List<ExtractedClause> clauses = geminiAiService.extractClauses(document);
            
            // Convert entity objects to response DTOs using Java 8 Streams
            List<ClauseResponse> clauseResponses = clauses.stream()
                    .map(ClauseResponse::new)  // Constructor reference - creates ClauseResponse from ExtractedClause
                    .collect(Collectors.toList());  // Collect results into a List
            
            return ResponseEntity.ok(clauseResponses);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error extracting clauses: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to ask questions about a specific document
     * 
     * @PostMapping: HTTP POST request to "/documents/{documentId}/question"
     * @Valid: Triggers validation of the request body
     * @RequestBody: Deserializes JSON request body into QuestionRequest object
     * @param documentId: ID of the document to ask about
     * @param request: Contains the question text
     * @param authentication: User authentication info
     * @return ResponseEntity: AI-generated answer or error message
     */
    @PostMapping("/documents/{documentId}/question")
    public ResponseEntity<?> askQuestion(@PathVariable Long documentId,
                                       @Valid @RequestBody QuestionRequest request,
                                       Authentication authentication) {
        try {
            // Verify document access
            Document document = getDocumentForUser(documentId, authentication);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            // Use AI service to answer the question about the document
            DocumentAnalysis analysis = geminiAiService.answerQuestion(document, request.getQuestion());
            return ResponseEntity.ok(new AnalysisResponse(analysis));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error answering question: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to generate legal document templates using AI
     * 
     * @PostMapping: HTTP POST request to "/templates/generate"
     * @param request: Contains template type and requirements
     * @param authentication: User authentication info
     * @return ResponseEntity: Generated template or error message
     */
    @PostMapping("/templates/generate")
    public ResponseEntity<?> generateTemplate(@Valid @RequestBody TemplateRequest request,
                                            Authentication authentication) {
        try {
            // Generate template using AI service
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

    /**
     * Endpoint to retrieve all analyses for a specific document
     * 
     * @GetMapping: HTTP GET request to "/documents/{documentId}/analyses"
     * @param documentId: ID of the document
     * @param authentication: User authentication info
     * @return ResponseEntity: List of document analyses or 404 if not found
     */
    @GetMapping("/documents/{documentId}/analyses")
    public ResponseEntity<?> getDocumentAnalyses(@PathVariable Long documentId,
                                                Authentication authentication) {
        // Verify document access
        Document document = getDocumentForUser(documentId, authentication);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        // Get all analyses for the document, ordered by creation date (newest first)
        List<DocumentAnalysis> analyses = analysisRepository.findByDocumentOrderByCreatedAtDesc(document);
        
        // Convert to response DTOs
        List<AnalysisResponse> analysisResponses = analyses.stream()
                .map(AnalysisResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(analysisResponses);
    }

    /**
     * Endpoint to retrieve all extracted clauses for a specific document
     * 
     * @GetMapping: HTTP GET request to "/documents/{documentId}/clauses"
     * @param documentId: ID of the document
     * @param authentication: User authentication info
     * @return ResponseEntity: List of extracted clauses or 404 if not found
     */
    @GetMapping("/documents/{documentId}/clauses")
    public ResponseEntity<?> getDocumentClauses(@PathVariable Long documentId,
                                              Authentication authentication) {
        // Verify document access
        Document document = getDocumentForUser(documentId, authentication);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        // Get clauses ordered by importance level (descending) then by creation date (descending)
        List<ExtractedClause> clauses = clauseRepository
                .findByDocumentOrderByImportanceLevelDescCreatedAtDesc(document);
        
        // Convert to response DTOs
        List<ClauseResponse> clauseResponses = clauses.stream()
                .map(ClauseResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(clauseResponses);
    }

    /**
     * Helper method to get a document and verify user has access to it
     * 
     * This method implements security by ensuring users can only access their own documents
     * 
     * @param documentId: ID of the document to retrieve
     * @param authentication: Spring Security authentication object
     * @return Document if found and accessible, null otherwise
     * @throws RuntimeException if user not found in database
     */
    private Document getDocumentForUser(Long documentId, Authentication authentication) {
        // Cast authentication principal to our custom UserPrincipal class
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Find the user in the database
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find document by ID and user (ensures user can only access their own documents)
        Optional<Document> documentOpt = documentRepository.findByIdAndUser(documentId, user);
        return documentOpt.orElse(null);  // Return null if document not found
    }

    // ==================== REQUEST DTOs (Data Transfer Objects) ====================
    
    /**
     * Request DTO for asking questions about documents
     * Contains the question text that will be sent to the AI service
     */
    public static class QuestionRequest {
        private String question;

        // Getter and setter methods for the question field
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
    }

    /**
     * Request DTO for generating document templates
     * Contains template type and specific requirements
     */
    public static class TemplateRequest {
        private String templateType;      // e.g., "contract", "agreement", "policy"
        private String requirements;      // Specific requirements for the template

        // Getters and setters
        public String getTemplateType() { return templateType; }
        public void setTemplateType(String templateType) { this.templateType = templateType; }

        public String getRequirements() { return requirements; }
        public void setRequirements(String requirements) { this.requirements = requirements; }
    }

    // ==================== RESPONSE DTOs (Data Transfer Objects) ====================
    
    /**
     * Response DTO for document analysis results
     * Converts DocumentAnalysis entity to a clean response format
     * 
     * This pattern separates internal entity structure from external API responses
     */
    public static class AnalysisResponse {
        private Long id;                    // Unique identifier
        private String analysisType;        // Type of analysis performed
        private String result;              // AI-generated result
        private String prompt;              // Prompt used for AI
        private Double confidenceScore;     // AI confidence in the result (0.0 to 1.0)
        private String status;              // Status of the analysis
        private String errorMessage;        // Error message if analysis failed
        private LocalDateTime createdAt;    // When the analysis was created

        /**
         * Constructor that converts DocumentAnalysis entity to response DTO
         * @param analysis: The entity object to convert
         */
        public AnalysisResponse(DocumentAnalysis analysis) {
            this.id = analysis.getId();
            this.analysisType = analysis.getAnalysisType().name();  // Convert enum to string
            this.result = analysis.getResult();
            this.prompt = analysis.getPrompt();
            this.confidenceScore = analysis.getConfidenceScore();
            this.status = analysis.getStatus().name();  // Convert enum to string
            this.errorMessage = analysis.getErrorMessage();
            this.createdAt = analysis.getCreatedAt();
        }

        // ==================== GETTERS AND SETTERS ====================
        // These methods allow JSON serialization/deserialization
        
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

    /**
     * Response DTO for extracted legal clauses
     * Converts ExtractedClause entity to a clean response format
     */
    public static class ClauseResponse {
        private Long id;                        // Unique identifier
        private String clauseType;              // Type of legal clause
        private String clauseText;              // Original clause text from document
        private String plainEnglishExplanation; // AI-generated explanation in plain English
        private String importanceLevel;         // Importance level of the clause
        private Double confidenceScore;         // AI confidence in the extraction
        private LocalDateTime createdAt;        // When the clause was extracted

        /**
         * Constructor that converts ExtractedClause entity to response DTO
         * @param clause: The entity object to convert
         */
        public ClauseResponse(ExtractedClause clause) {
            this.id = clause.getId();
            this.clauseType = clause.getClauseType();
            this.clauseText = clause.getClauseText();
            this.plainEnglishExplanation = clause.getPlainEnglishExplanation();
            this.importanceLevel = clause.getImportanceLevel().name();  // Convert enum to string
            this.confidenceScore = clause.getConfidenceScore();
            this.createdAt = clause.getCreatedAt();
        }

        // ==================== GETTERS AND SETTERS ====================
        
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
