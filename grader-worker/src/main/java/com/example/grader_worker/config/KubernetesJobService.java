package com.example.grader_worker.config;

import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

@Service
public class KubernetesJobService {

    private final KubernetesClient client;

    public KubernetesJobService(KubernetesClient client) {
        this.client = client;
    }

    public void createTestJob(Long submissionId, Long gradingRunId) {
        client.batch()
            .v1()
            .jobs()
            .inNamespace("grader")
            .resource(new JobBuilder()
                .withNewMetadata()
                    .withName("grading-" + submissionId)
                    .addToLabels("app", "grader-job")
                .endMetadata()
                .withNewSpec()
                    .withTtlSecondsAfterFinished(60)
                    .withBackoffLimit(0)
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("app", "grader-job")
                        .endMetadata()
                        .withNewSpec()
                            .withRestartPolicy("Never")
                            .addNewContainer()
                                .withName("grader")
                                .withImage("grader-test-job:dev")
                                .addNewEnv()
                                    .withName("SUBMISSION_ID")
                                    .withValue(String.valueOf(submissionId))
                                .endEnv()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build())
            .create();
    }
}
