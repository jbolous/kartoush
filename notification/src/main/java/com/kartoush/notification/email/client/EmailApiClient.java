package com.kartoush.notification.email.client;

import com.kartoush.notification.email.EmailMessage;

import java.util.Optional;

public interface EmailApiClient {

    Optional<String> send(EmailMessage email);
}
