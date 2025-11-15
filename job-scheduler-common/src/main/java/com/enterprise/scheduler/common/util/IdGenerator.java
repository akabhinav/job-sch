package com.enterprise.scheduler.common.util;

import java.util.UUID;

/**
 * Utility class for generating unique identifiers
 */
public class IdGenerator {

    private static final String PREFIX_JOB = "job";
    private static final String PREFIX_EXECUTION = "exec";
    private static final String PREFIX_SCHEDULE = "sched";

    public static String generateJobId() {
        return generate(PREFIX_JOB);
    }

    public static String generateExecutionId() {
        return generate(PREFIX_EXECUTION);
    }

    public static String generateScheduleId() {
        return generate(PREFIX_SCHEDULE);
    }

    public static String generate(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
