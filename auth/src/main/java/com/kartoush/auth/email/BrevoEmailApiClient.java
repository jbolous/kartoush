package com.kartoush.auth.email;

import java.util.Optional;

public interface BrevoEmailApiClient {

    Optional<String> send(EmailMessage email);
}
