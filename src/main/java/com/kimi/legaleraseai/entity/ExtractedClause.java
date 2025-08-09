package com.kimi.legaleraseai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "extracted_clauses")
@EntityListeners(AuditingEntityListener.class)
public class ExtractedClause {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clause_type", nullable = false)
    private String clauseType;

    @Lob
    @Column(name = "clause_text", nullable = false)
    private String clauseText;

    @Column(name = "start_position")
    private Integer startPosition;

    @Column(name = "end_position")
    private Integer endPosition;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "importance_level")
    @Enumerated(EnumType.STRING)
    private ImportanceLevel importanceLevel = ImportanceLevel.MEDIUM;

    @Lob
    @Column(name = "plain_english_explanation")
    private String plainEnglishExplanation;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @NotNull
    private Document document;

    public ExtractedClause() {}

    public ExtractedClause(String clauseType, String clauseText, Document document) {
        this.clauseType = clauseType;
        this.clauseText = clauseText;
        this.document = document;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClauseType() {
        return clauseType;
    }

    public void setClauseType(String clauseType) {
        this.clauseType = clauseType;
    }

    public String getClauseText() {
        return clauseText;
    }

    public void setClauseText(String clauseText) {
        this.clauseText = clauseText;
    }

    public Integer getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Integer startPosition) {
        this.startPosition = startPosition;
    }

    public Integer getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(Integer endPosition) {
        this.endPosition = endPosition;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public ImportanceLevel getImportanceLevel() {
        return importanceLevel;
    }

    public void setImportanceLevel(ImportanceLevel importanceLevel) {
        this.importanceLevel = importanceLevel;
    }

    public String getPlainEnglishExplanation() {
        return plainEnglishExplanation;
    }

    public void setPlainEnglishExplanation(String plainEnglishExplanation) {
        this.plainEnglishExplanation = plainEnglishExplanation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public enum ImportanceLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
