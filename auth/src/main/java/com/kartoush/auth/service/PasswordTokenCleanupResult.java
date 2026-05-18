package com.kartoush.auth.service;

public record PasswordTokenCleanupResult(long passwordResetDeletedCount, long passwordSetupDeletedCount) {
}
