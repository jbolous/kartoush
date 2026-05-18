package com.kartoush.customer.service;

import java.time.Instant;

public interface ActivationTokenCleanupService {

    long deleteExpiredTokens(Instant expiresBefore);
}
