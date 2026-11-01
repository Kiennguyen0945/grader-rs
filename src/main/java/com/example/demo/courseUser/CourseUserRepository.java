package com.example.demo.courseUser;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseUserRepository extends JpaRepository<CourseUser, Long> {
    boolean existsByCourseIdAndUserId(Long courseId, Long userId);
}

