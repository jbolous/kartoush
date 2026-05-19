package com.kartoush.customer.service.job;

import com.kartoush.platform.jobs.JobRequest;

public record WelcomeEmailJobRequest(String customerId) implements JobRequest {
}
