package com.compass.domain.user.repository;

import com.compass.domain.user.entity.User;
import com.compass.domain.user.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("userDomainPreferenceRepository") // Bean 이름 충돌을 피하기 위해 별명 지정
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    List<UserPreference> findByUser(User user);
    void deleteByUserAndPreferenceType(User user, String preferenceType);
}