package com.kimi.legaleraseai.repository;

import com.kimi.legaleraseai.entity.Document;
import com.kimi.legaleraseai.entity.ExtractedClause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtractedClauseRepository extends JpaRepository<ExtractedClause, Long> {
    List<ExtractedClause> findByDocumentOrderByImportanceLevelDescCreatedAtDesc(Document document);
    
    List<ExtractedClause> findByDocumentAndClauseTypeOrderByCreatedAtDesc(Document document, String clauseType);
    
    List<ExtractedClause> findByDocumentAndImportanceLevelOrderByCreatedAtDesc(Document document, 
                                                                              ExtractedClause.ImportanceLevel importanceLevel);
}
