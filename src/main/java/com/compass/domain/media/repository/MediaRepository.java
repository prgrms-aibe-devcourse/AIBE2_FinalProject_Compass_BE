package com.compass.domain.media.repository;

import com.compass.domain.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    
    List<Media> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId);
    
    Optional<Media> findByIdAndDeletedFalse(Long id);
    
    Optional<Media> findByIdAndUserIdAndDeletedFalse(Long id, String userId);
    
    @Query("SELECT m FROM Media m WHERE m.userId = :userId AND m.deleted = false")
    List<Media> findActiveMediaByUserId(@Param("userId") String userId);
    
    long countByUserIdAndDeletedFalse(String userId);
}