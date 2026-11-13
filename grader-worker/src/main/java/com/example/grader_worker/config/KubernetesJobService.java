package com.example.grader_worker.config;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import com.example.grader_worker.model.GradingResultResponse;
import tools.jackson.databind.ObjectMapper;


@Service
public class KubernetesJobService {

    private final KubernetesClient client;

    public KubernetesJobService(KubernetesClient client) {
        this.client = client;
    }

    public GradingResultResponse createJob(
        Long submissionId,
        Long gradingRunId,
        String requestJson) {

            String jobName = "grading-" + submissionId + "-" + gradingRunId;
            String configMapName = jobName + "-input";
            ObjectMapper objectMapper = new ObjectMapper();

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
                                    .withImage("grader-runner:dev")
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
            Job completedJob = client.batch()
                                .v1()
                                .jobs()
                                .inNamespace("grader")
                                .withName(jobName)
                                .waitUntilCondition(job -> {
                                    if (job == null || job.getStatus() == null || job.getStatus().getConditions() == null) {
                                        return false;
                                    }
                                    return job.getStatus().getConditions().stream()
                                    .anyMatch(c -> ("Complete".equals(c.getType()) || "Failed".equals(c.getType()))
                                                && "True".equals(c.getStatus()));
                                }, 2, TimeUnit.MINUTES);
            String output = client.pods()
                .inNamespace("grader")
                .withLabel("job-name", jobName)
                .list()
                .getItems()
                .stream()
                .findFirst()
                .map(pod -> client.pods()
                    .inNamespace("grader")
                    .withName(pod.getMetadata().getName())
                    .inContainer("grader")
                    .getLog()).orElseThrow(() -> new RuntimeException("Pod not found for job: " + jobName));
            // Use completedJob to verify it actually succeeded before parsing JSON
            boolean succeeded = completedJob.getStatus() != null
                && completedJob.getStatus().getSucceeded() != null
                && completedJob.getStatus().getSucceeded() > 0;
            System.out.print("Got the result");
            if (!succeeded) {
                throw new RuntimeException("Grading job failed (" + jobName + "). Logs:\n" + output);
            }

            return objectMapper.readValue(output.trim(), GradingResultResponse.class);

        }
}
