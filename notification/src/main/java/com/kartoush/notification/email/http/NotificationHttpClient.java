package com.kartoush.notification.email.http;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface NotificationHttpClient {

    HttpResponse<String> send(HttpRequest request, String provider);
}
