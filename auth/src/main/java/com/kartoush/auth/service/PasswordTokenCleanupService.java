package com.kartoush.auth.service;

import java.time.Instant;

public interface PasswordTokenCleanupService {

    PasswordTokenCleanupResult deleteExpiredTokens(Instant expiresBefore);
}
