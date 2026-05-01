package com.kartoush.notification.email.config;

import java.net.URI;

final class NotificationPropertyValidator {

    private NotificationPropertyValidator() {
    }

    static void validateRequiredText(final String value, final String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " must not be blank");
        }
    }

    static void validateHttpUrl(final String value, final String property) {
        validateRequiredText(value, property);

        final URI uri;
        try {
            uri = URI.create(value);
        } catch (final IllegalArgumentException exception) {
            throw new IllegalStateException(property + " must be a valid URI", exception);
        }

        final String scheme = uri.getScheme();
        if (!uri.isAbsolute() || uri.getHost() == null || scheme == null
            || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalStateException(property + " must be an absolute http or https URI");
        }
    }
}
