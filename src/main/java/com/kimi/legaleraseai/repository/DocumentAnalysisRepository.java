package com.kimi.legaleraseai.repository;

import com.kimi.legaleraseai.entity.Document;
import com.kimi.legaleraseai.entity.DocumentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentAnalysisRepository extends JpaRepository<DocumentAnalysis, Long> {
    List<DocumentAnalysis> findByDocumentOrderByCreatedAtDesc(Document document);
    
    Optional<DocumentAnalysis> findByDocumentAndAnalysisType(Document document, 
                                                           DocumentAnalysis.AnalysisType analysisType);
    
    List<DocumentAnalysis> findByDocumentAndAnalysisTypeOrderByCreatedAtDesc(Document document, 
                                                                            DocumentAnalysis.AnalysisType analysisType);
}
