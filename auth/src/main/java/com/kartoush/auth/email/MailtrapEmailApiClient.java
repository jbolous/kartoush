package com.kartoush.auth.email;

import java.util.Optional;

public interface MailtrapEmailApiClient {

    Optional<String> send(EmailMessage email);
}
