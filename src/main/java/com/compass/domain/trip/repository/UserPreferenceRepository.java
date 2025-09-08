package com.compass.domain.trip.repository;

import com.compass.domain.trip.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 선호도 Repository
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    /**
     * 사용자 ID와 선호도 타입으로 선호도 목록 조회
     * 
     * @param userId 사용자 ID
     * @param preferenceType 선호도 타입
     * @return 선호도 목록
     */
    List<UserPreference> findByUserIdAndPreferenceType(Long userId, String preferenceType);

    /**
     * 사용자 ID, 선호도 타입, 선호도 키로 특정 선호도 조회
     * 
     * @param userId 사용자 ID
     * @param preferenceType 선호도 타입
     * @param preferenceKey 선호도 키
     * @return 선호도 Optional
     */
    Optional<UserPreference> findByUserIdAndPreferenceTypeAndPreferenceKey(
            Long userId, String preferenceType, String preferenceKey);

    /**
     * 사용자 ID로 모든 선호도 조회
     * 
     * @param userId 사용자 ID
     * @return 선호도 목록
     */
    List<UserPreference> findByUserId(Long userId);

    /**
     * 사용자의 여행 스타일 선호도 조회
     * 
     * @param userId 사용자 ID
     * @return 여행 스타일 선호도 목록
     */
    @Query("SELECT up FROM UserPreference up " +
           "WHERE up.userId = :userId AND up.preferenceType = 'TRAVEL_STYLE' " +
           "ORDER BY up.preferenceValue DESC")
    List<UserPreference> findTravelStylePreferencesByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 특정 타입 선호도 존재 여부 확인
     * 
     * @param userId 사용자 ID
     * @param preferenceType 선호도 타입
     * @return 존재 여부
     */
    boolean existsByUserIdAndPreferenceType(Long userId, String preferenceType);

    /**
     * 사용자의 특정 타입 선호도 모두 삭제
     * 
     * @param userId 사용자 ID
     * @param preferenceType 선호도 타입
     */
    @Modifying
    @Query("DELETE FROM UserPreference up " +
           "WHERE up.userId = :userId AND up.preferenceType = :preferenceType")
    void deleteByUserIdAndPreferenceType(@Param("userId") Long userId, 
                                       @Param("preferenceType") String preferenceType);

    /**
     * 사용자의 여행 스타일 선호도 개수 조회
     * 
     * @param userId 사용자 ID
     * @return 선호도 개수
     */
    @Query("SELECT COUNT(up) FROM UserPreference up " +
           "WHERE up.userId = :userId AND up.preferenceType = 'TRAVEL_STYLE'")
    Long countTravelStylePreferencesByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 여행 스타일 선호도 가중치 합계 조회
     * 
     * @param userId 사용자 ID
     * @return 가중치 합계
     */
    @Query("SELECT COALESCE(SUM(up.preferenceValue), 0) FROM UserPreference up " +
           "WHERE up.userId = :userId AND up.preferenceType = 'TRAVEL_STYLE'")
    Double getTravelStyleWeightSumByUserId(@Param("userId") Long userId);
}
