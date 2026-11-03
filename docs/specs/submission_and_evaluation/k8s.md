## Goals

Implement k8s to my current project
500 words, 4 weeks. It is the limit not a suggestion
This spec is used to make the first implementation of k8s in my current project, it doesn't need to be perfect. It should be good enough for the first version

## Demo senarios

Scene 1:
When the submission and gradingRun are sent to the backend
- Create a job for the submission
- Return results

Scene 2:
When 10 submisison and gradingRun are sent at the same time
- Create 10 jobs for each submisison
- Grade
- Return results


## Why?

I am running untrusted code directly in docker container

## The flow

Backend sents Submisison and GradingRun -> RabbitMQ > My Grading Service -> k8s -> create job -> job creates a pod -> the pod runs code and returns in JSON format in stdout -> grading service saves to database
What is Deployment Pod:
Long-running -> grader worker waits for messages from RabbitMQ
grader-worker
What is a Job:
Short-lived -> run and deleted by k8s after finishing a job: 60 seconds
What is a Job Pod:
Short-lived -> created by job to complete a specific task


The existing GradingService will create the job because it receives the message from RabbitMQ first

Data is passed directly from the database -> job -> pod

Job receives source code through db ID

When a job failed to start, timemout, deleted -> Submisison Status = SYSTEM_ERROR