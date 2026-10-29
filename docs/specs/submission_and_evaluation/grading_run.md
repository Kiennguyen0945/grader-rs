

The flow:
    Backend
    Request -> backend service -> create Submission -> create GradingRun(status = QUEUED, submission = submission)
    send GradingRunId and SubmissionId -> rabbitMQ exchange -> submission.queue
    
    Grader Worker:
    rabbitMQ message -> consume submissionId + gradingRunId
    
    load GradingRun

    verify GradingRun belongs to Submission

    Sets GradingRun and Submission as Running

    create one SubmissionTest per assignment test
    ↓
    for each test:
        set SubmissionTest.gradingRun = gradingRun
        execute test
        update result/status/output/time/memory

    calculate grade:

    set GradingRun = COMPLETED
    set GradingRun.grade
    set GradingRun.finishedAt
    set Submission final status and grade








Yes. Before changing `GradingService`, answer these questions. They will determine the correct implementation.

### Ownership and lifecycle

1. Who creates the `GradingRun`?

   - The main application when the submission is created?
   - Or the worker when grading begins?

   Recommended: the main application creates a `QUEUED` run, and the worker only updates it to `RUNNING`, `COMPLETED`, or `FAILED`.

2. How does the worker find the correct run?

   - Does the RabbitMQ message contain only `submissionId`?
   - Should the worker query the latest queued run for that submission?
   - Should the message contain both `submissionId` and `gradingRunId`?

   Recommended: include both IDs in the message. This avoids accidentally grading the wrong run.

3. Can one submission have multiple runs?

   If yes, the worker must never calculate a new run ID using:

   ```java
   MAX(id) + 1
   ```

   That is unsafe if two workers run concurrently. The database should generate the `GradingRun.id`.

### Test-result creation

4. When are `SubmissionTest` records created?

   - When the `GradingRun` is created?
   - Or when the worker starts grading?

   Recommended for your current design: the worker creates one `SubmissionTest` for each assignment test after loading the `GradingRun`.

5. Should each `SubmissionTest` contain only:

   ```java
   Test test;
   GradingRun gradingRun;
   ```

   Recommended: yes. The submission can be reached through:

   ```java
   submissionTest.getGradingRun().getSubmission()
   ```

6. Should the worker save all test results before execution, with status `QUEUED`?

   Recommended: yes. This gives the frontend a predictable list of tests immediately.

### Status transitions

7. What status should the `GradingRun` have at each stage?

   A clear lifecycle could be:

   ```text
   QUEUED
      ↓
   RUNNING
      ↓
   COMPLETED or FAILED
   ```

8. What happens if compilation fails?

   - Should the `GradingRun` be `COMPLETED` with grade `0`?
   - Or `FAILED`?

   Recommended: use `COMPLETED` when grading finished normally, even if the submission has a compile error. Reserve `FAILED` for worker/system failures.

9. What happens to tests that were not executed?

   Recommended: mark them `NOT_RUN`.

10. If the worker is interrupted or crashes, who marks the run as `FAILED`?

   The worker should do this in a `catch`/`finally` path where possible, but a production system also needs recovery for workers that disappear entirely.

### Persistence and transactions

11. Should the entire grading process be one transaction?

   No. Do not keep one database transaction open while compiling and executing code.

   Prefer short transactions:

   ```text
   load run
   mark run RUNNING
   save

   create test results
   save

   update each result
   save

   mark run COMPLETED/FAILED
   save
   ```

12. Should `Submission.status` and `GradingRun.status` be updated separately?

   Decide the source of truth. A reasonable choice is:

   - `GradingRun.status`: status of this grading attempt
   - `Submission.status`: latest overall result for the submission

13. What grade belongs to `GradingRun`?

   The grade calculated for that specific run. If regraded, the old run’s grade must remain unchanged.

### Data consistency

14. How do you guarantee that every `SubmissionTest` belongs to the same submission as its `GradingRun`?

   If `SubmissionTest` only stores `gradingRun`, this is naturally enforced through the object model. Avoid adding a second direct `submission` field unless you need it for performance.

15. Should duplicate test results be prevented?

   Yes. Your unique constraint is appropriate:

   ```java
   (test_id, grading_run_id)
   ```

16. What happens if the worker receives the same message twice?

   This can happen with message queues. Decide whether the worker should:

   - Ignore a run already marked `COMPLETED`
   - Resume a `RUNNING` run
   - Create a new run

   Recommended initially: ignore already completed runs and log the duplicate message.

### Important changes suggested by your current code

Your current worker still uses the old model:

```java
st.submission
st.gradingRunId
```

After switching to relationships, queries should use paths such as:

```java
st.gradingRun.submission.id
st.gradingRun.id
```

Also, this method is unsafe:

```java
MAX(gradingRunId) + 1
```

Do not generate grading-run IDs manually. Create and save a `GradingRun`, then use its generated ID:

```java
GradingRun gradingRun = gradingRunRepository.save(
    new GradingRun(submission, null, "Initial grading")
);

Long gradingRunId = gradingRun.getId();
```

The most important beginner-friendly design is:

```text
SubmissionService:
    create Submission
    create GradingRun QUEUED
    send submissionId + gradingRunId to worker

GradingService:
    load GradingRun
    mark RUNNING
    create SubmissionTest records
    execute tests
    update results
    calculate grade
    mark run COMPLETED or FAILED
```

That gives each grading attempt its own identity and makes regrading much easier later.