package com.example.grader_worker.assignment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    @Query("""
    SELECT a
    FROM Assignment a
    LEFT JOIN FETCH a.tests
    WHERE a.id = :assignmentId
    """)
    Optional<Assignment> findByIdWithTests(@Param("assignmentId") Long assignmentId);
} 
