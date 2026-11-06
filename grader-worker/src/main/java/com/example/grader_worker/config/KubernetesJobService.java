package com.example.grader_worker.config;

import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

@Service
public class KubernetesJobService {

    private final KubernetesClient client;

    public KubernetesJobService(KubernetesClient client) {
        this.client = client;
    }

    public void createJob(
        Long submissionId,
        Long gradingRunId,
        String requestJson) {

            String jobName = "grading-" + submissionId + "-" + gradingRunId;
            String configMapName = jobName + "-input";

            client.configMaps()
                .inNamespace("grader")
                .resource(new ConfigMapBuilder()
                    .withNewMetadata()
                        .withName(configMapName)
                        .addToLabels("app", "grader-job")
                    .endMetadata()
                    .addToData("request.json", requestJson)
                    .build())
                .create();

            client.batch()
                .v1()
                .jobs()
                .inNamespace("grader")
                .resource(new JobBuilder()
                    .withNewMetadata()
                        .withName(jobName)
                    .endMetadata()
                    .withNewSpec()
                        .withBackoffLimit(0)
                        .withNewTemplate()
                            .withNewSpec()
                                .withRestartPolicy("Never")
                                .addNewContainer()
                                    .withName("grader")
                                    .withImage("grader-test-job:dev")
                                    .addNewVolumeMount()
                                        .withName("grading-input")
                                        .withMountPath("/input")
                                        .withReadOnly()
                                    .endVolumeMount()
                                .endContainer()
                                .addNewVolume()
                                    .withName("grading-input")
                                    .withNewConfigMap()
                                        .withName(configMapName)
                                    .endConfigMap()
                                .endVolume()
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                    .build())
                .create();
        }
}
