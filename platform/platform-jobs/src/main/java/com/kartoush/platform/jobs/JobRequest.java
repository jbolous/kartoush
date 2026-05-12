package com.kartoush.platform.jobs;

/**
 * Marker interface for durable background job requests.
 *
 * <p>Implementations should carry stable identifiers and simple scalar data
 * only. They should not embed rich domain objects or framework types.
 */
public interface JobRequest {
}
