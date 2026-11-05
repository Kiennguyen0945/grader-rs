package com.example.demo.courseUser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseUserRepository extends JpaRepository<CourseUser, Long> {
    boolean existsByCourseIdAndUserId(Long courseId, Long userId);
    Optional<CourseUser> findByCourseIdAndUserId(Long courseId, Long userId);
    List<CourseUser> findByCourseId(Long courseId);
    long countByCourseId(Long courseId);
}

