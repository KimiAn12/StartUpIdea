package com.kimi.legaleraseai.repository;

import com.kimi.legaleraseai.entity.Document;
import com.kimi.legaleraseai.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    List<Document> findByUserOrderByCreatedAtDesc(User user);
    
    Optional<Document> findByIdAndUser(Long id, User user);
    
    @Query("SELECT d FROM Document d WHERE d.user = :user AND " +
           "(LOWER(d.originalName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.extractedText) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Document> findByUserAndSearchTerm(@Param("user") User user, 
                                          @Param("searchTerm") String searchTerm, 
                                          Pageable pageable);
    
    long countByUser(User user);
}
