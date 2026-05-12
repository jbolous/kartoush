package com.kartoush.platform.jobs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobSchedulingExceptionTest {

    @Test
    void shouldPreserveMessageAndCause() {
        final IllegalStateException cause = new IllegalStateException("boom");

        final JobSchedulingException exception =
            new JobSchedulingException("Failed to schedule job", cause);

        assertThat(exception).hasMessage("Failed to schedule job");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
