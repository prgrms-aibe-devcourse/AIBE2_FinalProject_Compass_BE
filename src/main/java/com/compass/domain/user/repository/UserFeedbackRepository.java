package com.compass.domain.user.repository;

import com.compass.domain.user.entity.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
}