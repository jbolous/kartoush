package com.kartoush.notification.email.template;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;

@Component
public class ThymeleafEmailTemplateRenderer {

    private final TemplateEngine htmlTemplateEngine;

    private final TemplateEngine textTemplateEngine;

    public ThymeleafEmailTemplateRenderer() {
        this.htmlTemplateEngine = templateEngine(TemplateMode.TEXT, ".html");
        this.textTemplateEngine = templateEngine(TemplateMode.TEXT, ".txt");
    }

    public String renderHtml(final String templateName, final Map<String, Object> variables) {
        return htmlTemplateEngine.process(templateName, context(variables)).trim();
    }

    public String renderText(final String templateName, final Map<String, Object> variables) {
        return textTemplateEngine.process(templateName, context(variables)).trim();
    }

    private TemplateEngine templateEngine(final TemplateMode templateMode, final String suffix) {
        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/email/");
        templateResolver.setSuffix(suffix);
        templateResolver.setTemplateMode(templateMode);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        final TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }

    private Context context(final Map<String, Object> variables) {
        final Context context = new Context();
        context.setVariables(variables);
        return context;
    }
}
