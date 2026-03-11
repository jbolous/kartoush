package com.kartoush.platform.validation;

public record ValidationError(
        String field,
        String message) {
}
