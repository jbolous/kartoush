package com.kartoush.notification.email.template;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class ClasspathEmailTemplateRenderer {

    public String render(final String templatePath, final Map<String, String> variables) {
        String rendered = loadTemplate(templatePath);

        for (final Map.Entry<String, String> variable : variables.entrySet()) {
            rendered = rendered.replace(token(variable.getKey()), variable.getValue());
        }

        if (rendered.contains("{{")) {
            throw new IllegalStateException("Email template contains unresolved placeholders: " + templatePath);
        }

        return rendered.trim();
    }

    private String loadTemplate(final String templatePath) {
        final ClassPathResource resource = new ClassPathResource(templatePath);

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new IllegalStateException("Unable to load email template: " + templatePath, exception);
        }
    }

    private String token(final String key) {
        return "{{" + key + "}}";
    }
}
