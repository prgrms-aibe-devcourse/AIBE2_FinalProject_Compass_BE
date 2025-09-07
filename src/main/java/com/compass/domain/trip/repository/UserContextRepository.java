package com.compass.domain.trip.repository;

import com.compass.domain.trip.entity.UserContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserContext Repository
 * REQ-DB-002: 사용자 컨텍스트 데이터 접근
 */
@Repository
public interface UserContextRepository extends JpaRepository<UserContext, Long> {
    
    /**
     * 사용자 ID로 컨텍스트 조회
     */
    Optional<UserContext> findByUserId(Long userId);
    
    /**
     * 동행 유형으로 사용자 찾기
     */
    List<UserContext> findByTravelCompanion(UserContext.TravelCompanion companion);
    
    /**
     * 연령대로 사용자 찾기
     */
    List<UserContext> findByAgeGroup(String ageGroup);
    
    /**
     * 아이 동반 여행자 찾기
     */
    List<UserContext> findByWithChildrenTrue();
    
    /**
     * 특정 언어 선호자 찾기
     */
    List<UserContext> findByLanguagePreference(String language);
    
    /**
     * 현재 여행 목적별 사용자 찾기
     */
    List<UserContext> findByCurrentTripPurpose(String purpose);
    
    /**
     * 계절 선호도로 사용자 찾기
     */
    List<UserContext> findBySeasonPreference(String season);
    
    /**
     * 특별 요구사항이 있는 사용자 찾기
     */
    @Query("SELECT uc FROM UserContext uc WHERE uc.specialRequirements IS NOT NULL")
    List<UserContext> findUsersWithSpecialRequirements();
    
    /**
     * 신체 조건이 있는 사용자 찾기
     */
    @Query("SELECT uc FROM UserContext uc WHERE uc.physicalCondition IS NOT NULL")
    List<UserContext> findUsersWithPhysicalConditions();
    
    /**
     * 사용자 ID 존재 여부 확인
     */
    boolean existsByUserId(Long userId);
    
    /**
     * 사용자 ID로 컨텍스트 삭제
     */
    void deleteByUserId(Long userId);
    
    /**
     * 동행 인원수 범위로 찾기
     */
    @Query("SELECT uc FROM UserContext uc WHERE uc.companionCount BETWEEN :min AND :max")
    List<UserContext> findByCompanionCountRange(@Param("min") Integer min, @Param("max") Integer max);
}