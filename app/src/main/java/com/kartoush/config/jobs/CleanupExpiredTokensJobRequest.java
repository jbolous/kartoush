package com.kartoush.config.jobs;

import com.kartoush.platform.jobs.JobRequest;

public record CleanupExpiredTokensJobRequest() implements JobRequest {
}
