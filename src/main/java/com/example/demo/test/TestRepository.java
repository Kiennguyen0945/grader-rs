package com.example.demo.test;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByAssignmentId(Long assignmentId); 
    List<Test> findByAssignmentIdAndIsPublicTrueOrderByDisplayOrderAsc(Long assignmentId);
    long countByAssignmentId(Long assignmentId);
    long countByAssignmentIdAndIsPublicTrue(Long assignmentId);
    Optional<Test> findByIdAndAssignmentId(Long id, Long assignmentId);

    /** Next display order; max+1 so deletions never collide with uk_test_assignment_order. */
    @Query("select coalesce(max(t.displayOrder), 0) from Test t where t.assignment.id = :assignmentId")
    int findMaxDisplayOrder(@Param("assignmentId") Long assignmentId);
}

