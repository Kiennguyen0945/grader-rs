package com.example.demo.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    boolean existsByCourseIdAndCode(Long courseId, String code);
    List<Assignment> findByCourseIdAndUserId(Long courseId, Long userId);
    List<Assignment> findByCourseId(Long courseId);
    List<Assignment> findByCourseIdAndAssignmentStatus(Long courseId, Assignment.AssignmentStatus status);
}
 
