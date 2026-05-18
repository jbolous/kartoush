package com.kartoush.config.jobs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kartoush.jobs.cleanup")
public class CleanupJobProperties {

    private boolean enabled = true;

    private String expiredTokenCron = "0 0 3 * * *";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getExpiredTokenCron() {
        return expiredTokenCron;
    }

    public void setExpiredTokenCron(final String expiredTokenCron) {
        this.expiredTokenCron = expiredTokenCron;
    }
}
