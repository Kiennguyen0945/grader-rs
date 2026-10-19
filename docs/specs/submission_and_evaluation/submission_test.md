Below is your spec organized without resolving contradictions or unanswered questions.

# SubmissionTest Specification

## 1. Ownership

### Who creates a `SubmissionTest`?

The grader-worker creates one `SubmissionTest` for each assignment test.

### When is it created?

The worker creates one `SubmissionTest` for every assignment test when grading starts, before compilation.

---

## 2. Statuses

The worker creates one `SubmissionTest` per assignment test when grading begins.

Initial status:

```text
QUEUED
```

### For a test

- `PASSED` means expected output matched.
- `WRONG_ANSWER` means output differed.
- `TIME_LIMIT_EXCEEDED` means the test exceeded its limit.
- `NOT_RUN` means the test could not execute.

### For a submission

- `ACCEPTED` means all tests passed.
- Otherwise, the overall status follows the defined failure-priority rule.

### Status flow

```text
QUEUED
  ↓
RUNNING
  ├── PASSED
  ├── WRONG_ANSWER
  ├── COMPILE_ERROR
  ├── TIME_LIMIT_EXCEEDED
  ├── MEMORY_LIMIT_EXCEEDED
  ├── RUNTIME_ERROR
  ├── SYSTEM_ERROR
  └── NOT_RUN
```

---

## 3. Compilation behavior

If compilation fails:

- Submission becomes `COMPILE_ERROR`.
- All `SubmissionTest` rows become `NOT_RUN`.
- Timing, memory, and output remain null.

If compilation succeeds:

- Each test becomes `RUNNING` before execution.
- Every test runs even if another test fails.
- Each result is saved independently.

Fields on compilation failure:

```text
actualOutput:
memoryUsedKb: null
executionTimeMs: null
```

### Open question

You wrote:

```text
I think yes. But I don't know if it is a best practice
```

Regarding whether `SubmissionTest` rows should be created when compilation fails.

---

## 4. Timeout behavior

Each test case has a separated status.

If one test case times out:

- That test case receives a timeout status.
- Other test cases will be run normally.

Fields on timeout:

```text
actualOutput:
memoryUsedKb: null
executionTimeMs: null
```

---

## 5. Running tests after failure

Do you run all tests after one fails?

Yes.

Each test has a separated status.

---

## 6. Recorded fields

`SubmissionTest` records:

```text
actualOutput
memoryUsedKb
executionTimeMs
```

---

## 7. Database model

```text
class SubmissionTest {
+ id : Long
+ submission_id : Long
+ test_id : Long
+ status : String
+ actual_output : Text
+ memory_used_kib : Long
+ execution_time_ms : Long
}
```

---

## 8. APIs

### Get test results

```http
GET /api/submissions/{submissionId}/tests
```

Example:

```json
[
  {
    "submissionId": 1,
    "testId": 1,
    "status": "ACCEPTED",
    "actualOutput": "Hello World",
    "memoryUsedKb": 33,
    "executionTimeMs": 95
  }
]
```

### Get submission result

```http
GET /api/submissions/{submissionId}
```

Example:

```json
{
  "submissionId": 15,
  "assignmentId": 7,
  "userId": 1,
  "sourceCode": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n    }\n}",
  "status": "ACCEPTED",
  "grade": 10.0,
  "attemptNumber": 14
}
```

---

## 9. Authorization

Who can view the results?

Lecturer and Student.

Current authorization decisions:

- A student can view only their own submissions.
- A lecturer can view submissions only for their courses.
- A lecturer can view source code and hidden test results.
- Students cannot see hidden test input and expected output.

---

## 10. Execution behavior questions

### 1. If compilation fails, do you create `SubmissionTest` rows?

I think yes. But I don't know if it is a best practice.

### 2. If yes, should their status be `NOT_RUN`?

Yes.

### 3. If one test gets `WRONG_ANSWER`, do later tests still run?

Yes.

### 4. If one test times out, do later tests still run?

Yes.

### 5. What happens if the worker crashes midway?

The message is kept in RabbitMQ and will be resent after a certain amount of time.

### 6. Can a submission be graded more than once?

Yes.

### 7. Should old `SubmissionTest` rows be deleted before retrying?

No, I think we'll update it.

But what is actually retrying? Is it like a rejudge feature?

---

## 11. Statistics

### 8. Is `executionTimeMs` per test or for the entire submission?

Per test.

### 9. Is memory measured per test or for the entire process?

Per test.

### 10. What unit does the worker guarantee for memory?

Unanswered.

### 11. What should happen when memory measurement is unavailable?

Return null.

### 12. Should actual output be truncated for very large output?

No.

---

## 12. Status and grading

### 13. Is `ACCEPTED` the right per-test status, or would `PASSED` be clearer?

`PASSED`.

### 14. How is the overall grade calculated?

Each test case is treated equally in scores for this implementation.

### 15. Does one failed test always produce grade 0?

Nope, I'll update it.

### 16. Can partial grades exist?

Yes, one digit after the floating point.

### 17. Which result takes priority if multiple failures occur?

The overall status is the first non-passing failure encountered by test order.

---

## 13. Real-time updates

### 22. Should the frontend see each test result as soon as it finishes?

Yes, polling for the first implementation.

### 23. Should the frontend see `QUEUED`, `RUNNING`, and final statuses?

Yes, real-time.

### 24. What happens if the browser disconnects and reconnects?

Polling is based on the most recent request.

The client will send a GET request after reconnecting.

### 25. Should updates use polling first or SSE?

Polling.

---

## 14. Unresolved items

These are still open without changing your decisions:

1. Whether `SubmissionTest` rows should be created when compilation fails.

Yes -> set to non_run

2. The exact behavior of RabbitMQ retry after a worker crash.

RabbitMQ will resend the message until it receive ACK from a worker

3. The meaning of retry versus rejudge.

Retry: System retry. For example, a worker crash
Rejudge: Business logic. This feature will be parekd

4. Whether old results are updated or preserved as history.

preserved as history

5. The unit guaranteed for memory usage.

KB -> KiB

6. Whether unlimited `actualOutput` is safe.

NO
actualOutput is limited to 64 KB

7. The exact partial-grade formula.

grade = passedTests / totalTests × 10

8. The exact behavior when multiple failure statuses occur.

The overall status is the first non-passing failure encountered by test order.


9. Whether per-test `COMPILE_ERROR` is needed when compilation happens before tests.

No, -> NOT_RUN

10. Whether the API should return `PASSED` or `ACCEPTED` for individual test results.

PASSED