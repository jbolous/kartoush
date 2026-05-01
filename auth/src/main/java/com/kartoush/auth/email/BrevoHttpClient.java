package com.kartoush.auth.email;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface BrevoHttpClient {

    HttpResponse<String> send(HttpRequest request);
}
