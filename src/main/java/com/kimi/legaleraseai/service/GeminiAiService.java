package com.kimi.legaleraseai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kimi.legaleraseai.entity.Document;
import com.kimi.legaleraseai.entity.DocumentAnalysis;
import com.kimi.legaleraseai.entity.ExtractedClause;
import com.kimi.legaleraseai.repository.DocumentAnalysisRepository;
import com.kimi.legaleraseai.repository.ExtractedClauseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiAiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiAiService.class);

    @Value("${google.gemini.api.key}")
    private String apiKey;

    @Value("${google.gemini.api.url}")
    private String apiUrl;

    @Autowired
    private DocumentAnalysisRepository analysisRepository;

    @Autowired
    private ExtractedClauseRepository clauseRepository;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiAiService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public DocumentAnalysis summarizeDocument(Document document) {
        String prompt = "Please provide a comprehensive summary of the following legal document in plain English. " +
                "Focus on the main purpose, key parties, important terms, and significant obligations or rights. " +
                "Make it accessible to someone without legal training:\n\n" + document.getExtractedText();

        try {
            String summary = callGeminiApi(prompt);
            DocumentAnalysis analysis = new DocumentAnalysis(
                    DocumentAnalysis.AnalysisType.SUMMARY,
                    summary,
                    document
            );
            analysis.setPrompt(prompt);
            return analysisRepository.save(analysis);
        } catch (Exception e) {
            logger.error("Error summarizing document: {}", e.getMessage(), e);
            DocumentAnalysis analysis = new DocumentAnalysis();
            analysis.setDocument(document);
            analysis.setAnalysisType(DocumentAnalysis.AnalysisType.SUMMARY);
            analysis.setStatus(DocumentAnalysis.AnalysisStatus.FAILED);
            analysis.setErrorMessage(e.getMessage());
            return analysisRepository.save(analysis);
        }
    }

    public List<ExtractedClause> extractClauses(Document document) {
        String prompt = "Analyze the following legal document and extract key clauses. " +
                "For each clause, provide: 1) Clause type (e.g., 'Payment Terms', 'Termination', 'Liability', etc.), " +
                "2) The exact text of the clause, 3) A plain English explanation, 4) Importance level (LOW/MEDIUM/HIGH/CRITICAL). " +
                "Format as JSON array with fields: clauseType, clauseText, explanation, importance.\n\n" +
                document.getExtractedText();

        try {
            String response = callGeminiApi(prompt);
            List<ExtractedClause> clauses = parseClausesFromResponse(response, document);
            return clauseRepository.saveAll(clauses);
        } catch (Exception e) {
            logger.error("Error extracting clauses: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public DocumentAnalysis answerQuestion(Document document, String question) {
        String prompt = "Based on the following legal document, please answer this question: " + question + 
                "\n\nProvide a clear, accurate answer based only on the information in the document. " +
                "If the answer is not found in the document, please state that clearly.\n\n" +
                "Document content:\n" + document.getExtractedText();

        try {
            String answer = callGeminiApi(prompt);
            DocumentAnalysis analysis = new DocumentAnalysis(
                    DocumentAnalysis.AnalysisType.QUESTION_ANSWER,
                    answer,
                    document
            );
            analysis.setPrompt(question);
            return analysisRepository.save(analysis);
        } catch (Exception e) {
            logger.error("Error answering question: {}", e.getMessage(), e);
            DocumentAnalysis analysis = new DocumentAnalysis();
            analysis.setDocument(document);
            analysis.setAnalysisType(DocumentAnalysis.AnalysisType.QUESTION_ANSWER);
            analysis.setStatus(DocumentAnalysis.AnalysisStatus.FAILED);
            analysis.setErrorMessage(e.getMessage());
            analysis.setPrompt(question);
            return analysisRepository.save(analysis);
        }
    }

    public DocumentAnalysis generateTemplate(String templateType, String requirements) {
        String prompt = "Generate a simple legal " + templateType + " template based on these requirements: " +
                requirements + "\n\n" +
                "Please provide a basic template with placeholder fields marked in [BRACKETS]. " +
                "Include standard clauses appropriate for this type of document. " +
                "Add a disclaimer that this is a basic template and legal review is recommended.";

        try {
            String template = callGeminiApi(prompt);
            DocumentAnalysis analysis = new DocumentAnalysis(
                    DocumentAnalysis.AnalysisType.TEMPLATE_GENERATION,
                    template,
                    null // No associated document for templates
            );
            analysis.setPrompt(prompt);
            return analysisRepository.save(analysis);
        } catch (Exception e) {
            logger.error("Error generating template: {}", e.getMessage(), e);
            DocumentAnalysis analysis = new DocumentAnalysis();
            analysis.setAnalysisType(DocumentAnalysis.AnalysisType.TEMPLATE_GENERATION);
            analysis.setStatus(DocumentAnalysis.AnalysisStatus.FAILED);
            analysis.setErrorMessage(e.getMessage());
            analysis.setPrompt(prompt);
            return analysisRepository.save(analysis);
        }
    }

    private String callGeminiApi(String prompt) throws Exception {
        Map<String, Object> requestBody = createGeminiRequestBody(prompt);
        
        String response = webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseGeminiResponse(response);
    }

    private Map<String, Object> createGeminiRequestBody(String prompt) {
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", Arrays.asList(part));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Arrays.asList(content));

        // Add safety settings and generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", 2048);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private String parseGeminiResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode candidates = root.path("candidates");
        
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            
            if (parts.isArray() && parts.size() > 0) {
                return parts.get(0).path("text").asText();
            }
        }
        
        throw new RuntimeException("Invalid response format from Gemini API");
    }

    private List<ExtractedClause> parseClausesFromResponse(String response, Document document) {
        List<ExtractedClause> clauses = new ArrayList<>();
        
        try {
            // Try to parse as JSON first
            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.isArray()) {
                for (JsonNode clauseNode : jsonResponse) {
                    ExtractedClause clause = createClauseFromJson(clauseNode, document);
                    if (clause != null) {
                        clauses.add(clause);
                    }
                }
            }
        } catch (Exception e) {
            // If JSON parsing fails, try to extract clauses using regex
            logger.warn("Failed to parse clauses as JSON, trying regex parsing: {}", e.getMessage());
            clauses = parseClausesWithRegex(response, document);
        }
        
        return clauses;
    }

    private ExtractedClause createClauseFromJson(JsonNode clauseNode, Document document) {
        try {
            String clauseType = clauseNode.path("clauseType").asText();
            String clauseText = clauseNode.path("clauseText").asText();
            String explanation = clauseNode.path("explanation").asText();
            String importance = clauseNode.path("importance").asText("MEDIUM");

            ExtractedClause clause = new ExtractedClause(clauseType, clauseText, document);
            clause.setPlainEnglishExplanation(explanation);
            clause.setImportanceLevel(ExtractedClause.ImportanceLevel.valueOf(importance.toUpperCase()));
            clause.setConfidenceScore(0.8); // Default confidence

            return clause;
        } catch (Exception e) {
            logger.error("Error creating clause from JSON: {}", e.getMessage());
            return null;
        }
    }

    private List<ExtractedClause> parseClausesWithRegex(String response, Document document) {
        List<ExtractedClause> clauses = new ArrayList<>();
        
        // Simple regex-based parsing as fallback
        Pattern pattern = Pattern.compile("(?i)(clause type|type):\\s*(.+?)\\n.*?(clause text|text):\\s*(.+?)\\n", 
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        
        while (matcher.find()) {
            String clauseType = matcher.group(2).trim();
            String clauseText = matcher.group(4).trim();
            
            ExtractedClause clause = new ExtractedClause(clauseType, clauseText, document);
            clause.setImportanceLevel(ExtractedClause.ImportanceLevel.MEDIUM);
            clause.setConfidenceScore(0.6); // Lower confidence for regex parsing
            clauses.add(clause);
        }
        
        return clauses;
    }
}
