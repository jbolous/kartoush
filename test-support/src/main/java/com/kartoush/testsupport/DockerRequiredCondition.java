package com.kartoush.testsupport;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

public class DockerRequiredCondition implements ExecutionCondition {
    @Override
    public @NonNull ConditionEvaluationResult evaluateExecutionCondition(@NonNull ExtensionContext context) {
        try {
            boolean available = DockerClientFactory.instance().isDockerAvailable();
            return available
                ? ConditionEvaluationResult.enabled("Docker is available")
                : ConditionEvaluationResult.disabled("Docker is not available, skipping integration tests");
        } catch (Exception e) {
            return ConditionEvaluationResult.disabled("Docker/Testcontainers not available: " + e.getMessage());
        }
    }

}
