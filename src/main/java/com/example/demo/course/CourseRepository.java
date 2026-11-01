package com.example.demo.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository
    extends JpaRepository<Course, Long> {
        Optional<Course> findByCode(String code);
        boolean existsByCode(String code);

        @Query("SELECT cu.course FROM CourseUser cu WHERE cu.user.id = :userId")
        List<Course> findEnrolledCoursesByUserId(@Param("userId") Long userId);
    }
