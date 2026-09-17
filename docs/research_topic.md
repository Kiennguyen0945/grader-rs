Building a multilingual automated source code distribution and grading system, integrated with secure isolation mechanisms on the Kubernetes platform.

Research and develop a distributed, highly scalable Auto-Grader application system to support teaching, practice, and online programming exams at universities. The system must thoroughly address congestion issues when a large number of submissions arrive simultaneously, while ensuring absolute infrastructure security through mechanisms that isolate malicious source code.

Requirements:

    Exam and assignment management: Instructors can create assignments, configure open/close times, limit the number of submissions, and set specific resource constraints for each task.

    Test Case management: The system allows uploading of Test Case sets (Input/Output). It supports separation between public Test Cases (visible to students for debugging) and hidden Test Cases (used for final grading).

    Submission interface & real-time results: Students can submit source code directly via the browser.

    Leaderboard & reporting: Automatically generates a class-wide grade summary and supports periodic data export for course evaluation record-keeping.

Technology stack:

    Backend: Spring Boot 3.x

    Kubernetes: Using Fabric8 Kubernetes Client or the Official Java Client for Kubernetes

    Message Queue: RabbitMQ (with Spring AMQP) or Apache Kafka (with Spring Kafka)

    Database: PostgreSQL with Spring Data JPA

    Real-time communication: