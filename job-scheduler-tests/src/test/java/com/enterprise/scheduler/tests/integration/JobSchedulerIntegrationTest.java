package com.enterprise.scheduler.tests.integration;

import com.enterprise.scheduler.api.JobSchedulerApplication;
import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.service.JobService;
import com.enterprise.scheduler.core.service.SchedulerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Job Scheduler
 */
@SpringBootTest(classes = JobSchedulerApplication.class)
@ActiveProfiles("test")
@Testcontainers
class JobSchedulerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_scheduler")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private JobService jobService;

    @Autowired
    private SchedulerService schedulerService;

    @Test
    void testCreateAndExecuteJob() {
        // Create job
        Job job = Job.builder()
                .name("test-job")
                .type("shell")
                .description("Test job")
                .enabled(true)
                .build();

        Map<String, Object> config = new HashMap<>();
        config.put("command", "echo 'Hello World'");
        job.setConfiguration(config);

        Job savedJob = jobService.createJob(job);

        assertThat(savedJob.getId()).isNotNull();
        assertThat(savedJob.getName()).isEqualTo("test-job");
        assertThat(savedJob.getStatus()).isEqualTo(JobStatus.SCHEDULED);

        // Execute job
        JobExecution execution = schedulerService.executeManually(savedJob.getId(), null);

        assertThat(execution).isNotNull();
        assertThat(execution.getJobId()).isEqualTo(savedJob.getId());
    }

    @Test
    void testJobLifecycle() {
        Job job = Job.builder()
                .name("lifecycle-test-job")
                .type("shell")
                .enabled(true)
                .build();

        // Create
        Job created = jobService.createJob(job);
        assertThat(created.isEnabled()).isTrue();

        // Disable
        Job disabled = jobService.disableJob(created.getId());
        assertThat(disabled.isEnabled()).isFalse();

        // Enable
        Job enabled = jobService.enableJob(created.getId());
        assertThat(enabled.isEnabled()).isTrue();

        // Delete
        jobService.deleteJob(created.getId());
    }
}
