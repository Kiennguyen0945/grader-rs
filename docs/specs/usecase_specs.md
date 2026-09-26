## UC-C01: Create Assignment
### Goals 
Allow a lecturer to create an assignment for one of their courses

### Actors
LECTURER

### Preconditions

- the lecturer is authenticated
- the lecturer has the 'LECTURER' role.
- the selected course exists.
- the lecturer manages the selected course

### Main Flows


1. The lecturer selects a course.
2. The lecturer enters the assignment code, title, description, opening time, closing time, submission limit, and programming language.
3. The lecturer submits the assignment.
4. The system validates the input.
5. The system creates the assignment and associates it with the selected course.
6. The system returns the created assignment.

### Alternative Flows

- If required data is missing, return validation errors.
- If the assignment code already exists, reject the request.
- If the closing time is earlier than the opening time, reject the request.
- If the lecturer does not manage the course, reject the request.
- If the database operation fails, return a system error.


### Business rules

- `title` is required.
- `programming_language` is required.
- `submission_limit` must be greater than zero.
- `closing_time`, if provided, must be later than `opening_time`.
- `opening_time` default to NOW() if omitted
- `submission_limit` 3 attempts as default
- `description` is optional
- `closing_time` is optional
- An assignment cannot be deleted after it has received a submission.
- If the lecturer enters the code field, validate uniqueness.
- Code can't be edited after it has received a submission
- System generates the assignment code but it can manually edited by lecturer before publishing
- An assignment draft can be edited
- An assignment status: DRAFT → PUBLISHED → CLOSED. The assignment with `CLOSED` status can be changed to `PUBLISHED` if the lecturer expands the `closing_time`

### Postconditions

- The assignment is stored in the database.
- The assignment is associated with the selected course.
- The assignment has status `DRAFT`.
- The lecturer can add test cases to the assignment.


### Endpoints

POST /api/courses/{courseId}/assignments

## UC-C0 : Publish Assignment

### Goals 

### Actors

### Preconditions

- A lecturer can publish an assignment only when it has valid data and at least one test case.

### Main Flows

### Alternative Flows

### Postcoditions

## UC-C02: Add Test Cases
### Goals 

Allow lecturer to create test cases and associates them to an assignment 

### Actors

LECTURER

### Preconditions

- The lecturer is authenticated
- The lecturer has the 'LECTURER' role.
- The selected assignment exists
- The lecturer manages the assignment.
- The assignment has not received a submission

### Main flow

1. The lecturer selects an assignment.
2. The lecturer enters test input and expected output.
3. The lecturer chooses whether the test is public or hidden.
4. The lecturer submits the test case.
5. The system validates the test case.
6. The system stores the test case for the assignment.
7. The system returns the created test case.

### Alternative Flows

- If input or expected output is missing, return a validation error.
- If the assignment has received submissions, reject changes to test cases.
- If the lecturer does not manage the assignment, reject the request.

### Postconditions

- The test case is stored.
- The test case is associated with the assignment.
- The test is marked as public or hidden.

### Endpoint

POST /api/assignments/{assignmentId}/tests

## UC-C03: Submit Source Code
### Goals 
Allow a student to submit source code for an open assignment

### Actors

STUDENT

### Preconditions

- The student is authenticated.
- The student has the `STUDENT` role.
- The assignment exists.
- The assignment is published.
- The current time is within the assignment submission period.
- The student belongs to the assignment's course.
- The student has not reached the submission limit.


### Main Flows

1. The student selects a course.
2. The student enters source code
3. The student submits the source code
4. The system validates the request
5. The system determines the attempt number.
6. The system stores the submission with status `QUEUED`.
7. The system publishes a grading message.
8. The system returns the submission ID and `QUEUED` status.


### Alternative Flows

- If the assignment is closed, reject the submission.
- If the submission limit is reached, reject the submission.
- If the source code is empty or too large, return a validation error.
- If the student is not enrolled in the course, reject the submission.
- If the queue is unavailable, return a system error.

### Postconditions

- A submission belongs to the student and assignment.
- The submission has a unique attempt number.
- The submission has status `QUEUED`.
- A grading task has been published.

### Endpoint

POST /api/assignments/{assignmentId}/submissions

## UC-C04: View Submission Result

### Goal

Allow a student to view the result of their own submission.

### Preconditions

- The student is authenticated.
- The submission exists.
- The submission belongs to the student.

### Main flow

1. The student requests a submission result.
2. The system verifies ownership.
3. The system returns the submission status, grade, and test results.
4. If grading is still in progress, the system returns `QUEUED` or `RUNNING`.

### Alternative flows

- If the submission does not exist, return not found.
- If the submission belongs to another student, return forbidden.
- If the result is not ready, return the current status.

### Postconditions

- The student can see only their own submission result.

### Endpoint

GET /api/submissions/{submissionId}



## UC-S01: Grade Submission

### Goals 

System processes and provides grade for a submission

### Actors

Grading Worker

### Trigger

The worker receives a grading message

### Preconditions

- The submission status is `QUEUED`


### Main Flows

### Alternative Flows

### Postcoditions