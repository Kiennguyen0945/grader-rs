Scope
- What is the smallest demonstrable version?
a toy grader with only 4-6 tables for business logics, so I can focus on other things like Kubernetes, RabbitMQ
- Which programming language will be supported first?
Java
- Is this a prototype, a research project, or production software?
It is a reasearch project that I need to submit on the current semester, it'll be envolve into my final project in may 2027
- How many users and submissions must it support?
a student
a lecturer
an admin
- What features are explicitly out of scope?
Multiple languages, sophicated bussiness logics, microservice, complicated kubernetes implementation
Impressing other people
- What must be finished by the final presentation?

The system must handle: 
Submit 20 jobs concurrently and verify that all are eventually processed without losing results
Support one worker initially and allow multiple worker instances to consume in the same queue 
A job should be executed in an isolated environment created by Kubernetes
System must use postgresSQL
Basic Authentication including register, login, logout
RabbitMQ submission queue
Kubernetes Job per submission
PostgreSQL
Basic WebSocket



Supporting Requirement: 
Docker, cicd, github actions

Users and permissions
- Who are the users?
lecturer
student
admin

- What can a lecturer do?
CRUD lectures

- What can a student do?
CRUD submissions


- Can administrators manage users?
CRUD user, assign permissions

- Can students see each other’s submissions?
Nope

- Who can see hidden test cases?

lecturer

- How is an assignment created?

lecturer -> frontend -> submit -> urls -> database -> response

- When can students submit?

Compliation success

- What happens after the deadline?

the assigment is locked, and cannot submit

- How many submissions are allowed?

three attempts

- Can an assignment be edited after students submit?

Yes.
If this assignment hasn't been validated and scored by the lecturer

- What happens when code is submitted?

Student Source Code -> Execution Sandbox (K8s Pod) 

- What statuses can a submission have?

QUEUED
  ↓
RUNNING
  ├── ACCEPTED
  ├── WRONG_ANSWER
  ├── COMPILE_ERROR
  ├── TIME_LIMIT_EXCEEDED
  ├── MEMORY_LIMIT_EXCEEDED
  ├── RUNTIME_ERROR
  └── SYSTEM_ERROR

- How are compilation errors represented?

After submission, at the bottom of the typing form

- What happens when code times out?

return submission with: TIME_LIMIT_EXCEEDED

- What resource limits apply?

Max 512MB RAM, 2 second

- How are multiple test cases scored?

Each test case is equally in score
If an assignment has 10 test cases:
pass 8 public cases + hidden cases = 8/10

Leaderboard using average score per assignment

- Can a submission be retried?

Yes, but a student can't retrieve other submissions

Security
- What code is considered untrusted?

Every code is considered untrusted and run in and isolated environment created by k8s

- Can submitted code access the internet?
Nope
- Can it access the host filesystem?
Nope
- What CPU, memory, disk, and execution-time limits apply?

Source code: maximum 100 KB
Output: maximum 1 MB
Compilation timeout: 10 seconds
Execution timeout: 2 seconds
CPU: 1 core
Memory: 512 MB
Workspace: 10 MB
Network: disabled
User: non-root

- What happens when a grading job crashes?

system_errors

- Who reviews the isolation design?

admin