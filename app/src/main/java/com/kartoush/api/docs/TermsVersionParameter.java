package com.kartoush.api.docs;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(
    name = "version",
    description = "Human-readable Terms of Service version",
    example = "2026.04.01",
    required = true
)
public @interface TermsVersionParameter {
}
