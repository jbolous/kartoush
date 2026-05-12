package com.kartoush.config.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.jobs.JobRequest;
import org.springframework.core.ResolvableType;

import java.util.List;

public class PlatformJobDispatcher {

    private final ObjectMapper objectMapper;

    private final List<JobHandler<?>> jobHandlers;

    public PlatformJobDispatcher(
        final ObjectMapper objectMapper,
        final List<JobHandler<?>> jobHandlers) {
        this.objectMapper = objectMapper;
        this.jobHandlers = List.copyOf(jobHandlers);
    }

    public void dispatch(final String requestType, final String payload) {
        final JobRequest request = deserialize(requestType, payload);
        final JobHandler<JobRequest> jobHandler = resolveHandler(request.getClass());
        jobHandler.handle(request);
    }

    private JobRequest deserialize(final String requestType, final String payload) {
        final Class<?> resolvedRequestType;
        try {
            resolvedRequestType = Class.forName(requestType);
        } catch (final ClassNotFoundException ex) {
            throw new IllegalStateException("Unknown job request type " + requestType, ex);
        }

        if (!JobRequest.class.isAssignableFrom(resolvedRequestType)) {
            throw new IllegalStateException("Resolved type does not implement JobRequest: " + requestType);
        }

        try {
            return (JobRequest) objectMapper.readValue(payload, resolvedRequestType);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize job request of type " + requestType, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private JobHandler<JobRequest> resolveHandler(final Class<?> requestType) {
        final List<JobHandler<?>> matchingHandlers = jobHandlers.stream()
            .filter(jobHandler -> requestType.equals(resolveHandledRequestType(jobHandler)))
            .toList();

        if (matchingHandlers.isEmpty()) {
            throw new IllegalStateException("No JobHandler registered for request type " + requestType.getName());
        }

        if (matchingHandlers.size() > 1) {
            throw new IllegalStateException("Multiple JobHandler beans registered for request type " + requestType.getName());
        }

        return (JobHandler<JobRequest>) matchingHandlers.getFirst();
    }

    private Class<?> resolveHandledRequestType(final JobHandler<?> jobHandler) {
        final ResolvableType resolvableType = ResolvableType.forInstance(jobHandler).as(JobHandler.class);
        final Class<?> resolvedRequestType = resolvableType.getGeneric(0).resolve();

        if (resolvedRequestType == null) {
            throw new IllegalStateException("Could not resolve JobHandler request type for " + jobHandler.getClass().getName());
        }

        return resolvedRequestType;
    }
}
