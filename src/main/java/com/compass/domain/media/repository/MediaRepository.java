package com.compass.domain.media.repository;

import com.compass.domain.media.entity.Media;
import com.compass.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    
    List<Media> findByUserAndDeletedFalseOrderByCreatedAtDesc(User user);
    
    Optional<Media> findByIdAndDeletedFalse(Long id);
    
    Optional<Media> findByIdAndUserAndDeletedFalse(Long id, User user);
    
    @Query("SELECT m FROM Media m WHERE m.user = :user AND m.deleted = false")
    List<Media> findActiveMediaByUser(@Param("user") User user);
    
    long countByUserAndDeletedFalse(User user);
    
    @Query("SELECT m FROM Media m WHERE m.user.id = :userId AND m.deleted = false ORDER BY m.createdAt DESC")
    List<Media> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    Optional<Media> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);
    
    long countByUserIdAndDeletedFalse(Long userId);
}