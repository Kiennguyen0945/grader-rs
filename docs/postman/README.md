# Grader-RS Postman Testing Guide

This directory contains an end-to-end Postman collection to test and demo the entire system if you ever need a headless fallback.

## Quick Start

1. Open Postman $\to$ Click **Import** $\to$ Select [`docs/postman/grader_e2e_collection.json`](./grader_e2e_collection.json).
2. The collection uses variables (`baseUrl`, `lecturerToken`, `studentToken`, `courseId`, `assignmentId`, `submissionId`).
3. Make sure your Docker containers are running:
   ```bash
   docker compose -f docker-compose.dev.yaml up
   ```

---

## Automated Test Sequence

The requests are numbered so you can run them sequentially or execute the whole suite via Postman Collection Runner:

| Step | Request Name | Method & Endpoint | What It Tests |
| :--- | :--- | :--- | :--- |
| **01** | **Lecturer Login** | `POST /api/auth/login` | Authenticates as lecturer `lady`. Automatically saves `{{lecturerToken}}` and parses `{{lecturerId}}`. |
| **02** | **Student Login** | `POST /api/auth/login` | Authenticates as student `dante`. Automatically saves `{{studentToken}}` and parses `{{studentId}}`. |
| **03** | **Lecturer Create Course** | `POST /api/courses` | Creates course `CS201`. Saves `{{courseId}}`. |
| **04** | **Lecturer Create Assignment** | `POST /api/courses/{{courseId}}/assignments` | Creates assignment `LAB1` with attempt limit = 3. Saves `{{assignmentId}}`. |
| **05** | **Lecturer Add Public Test** | `POST /api/assignments/{{assignmentId}}/tests` | Adds sample public test case (`"hello world"` $\to$ `"hello world"`). |
| **06** | **Student Get Enrolled Courses**| `GET /api/courses` | Verifies student course filtering (only returns enrolled courses). |
| **07** | **Student Get Course Assignments** | `GET /api/courses/{{courseId}}/assignments` | Returns only `PUBLISHED` assignments for enrolled students. |
| **08** | **Student Get Workspace Detail** | `GET /api/assignments/{{assignmentId}}` | Verifies public sample test is returned while hidden tests are excluded. |
| **09** | **Student Submit Solution** | `POST /api/assignments/{{assignmentId}}/submissions` | Submits source code (no `userId` in body). Verifies status is `QUEUED`. Saves `{{submissionId}}`. |
| **10** | **Student Poll Verdict** | `GET /api/submissions/{{submissionId}}` | Queries verdict after worker processes RabbitMQ queue (`ACCEPTED`, `grade: 10.0`). |
| **11** | **Security Boundary Check** | `POST /api/courses` (using `studentToken`) | Asserts `403 Forbidden` when student attempts course creation. |

---

## Testing Special Grading Verdicts

To test compiler errors or timeout limits in step **09**, simply replace the `sourceCode` in the request body:

### Time Limit Exceeded (TLE)
```json
{
  "sourceCode": "public class Main { public static void main(String[] args) { while(true) {} } }"
}
```

### Compile Error
```json
{
  "sourceCode": "public class Main { syntax error }"
}
```

