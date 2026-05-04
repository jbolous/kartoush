package com.kartoush.notification.email.delivery;

import com.kartoush.notification.email.EmailMessage;

public interface EmailDeliveryService {

    void send(EmailMessage email);
}
