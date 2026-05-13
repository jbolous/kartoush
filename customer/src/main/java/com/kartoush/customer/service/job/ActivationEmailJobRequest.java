package com.kartoush.customer.service.job;

import com.kartoush.platform.jobs.JobRequest;

public record ActivationEmailJobRequest(
    String customerId,
    String rawToken
) implements JobRequest {
}
