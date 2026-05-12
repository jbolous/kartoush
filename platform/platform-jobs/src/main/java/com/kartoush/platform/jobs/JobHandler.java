package com.kartoush.platform.jobs;

/**
 * Handles one typed background job request.
 *
 * <p>Infrastructure implementations are expected to invoke handlers after
 * resolving the matching {@link JobRequest} type.
 *
 * @param <T> request type handled by this handler
 */
public interface JobHandler<T extends JobRequest> {

    void handle(T request);
}
