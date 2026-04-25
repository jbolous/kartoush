package com.kartoush.api.docs;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(
    name = "termsOfServiceId",
    description = "Terms of Service ULID identifier",
    example = "01KQ0INTERNALTERMS000000001",
    required = true
)
public @interface TermsOfServiceIdParameter {
}
