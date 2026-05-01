package com.kartoush.auth.email;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface MailtrapHttpClient {

    HttpResponse<String> send(HttpRequest request);
}
