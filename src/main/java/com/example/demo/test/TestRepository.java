package com.example.demo.test;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByAssignmentId(Long assignmentId); 
    int countByAssignmentId(Long assignmentId);
}
