package de.swiftbyte.gmc.backend.expressions;

import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.expressions.context.AutomationEvalContext;
import de.swiftbyte.gmc.backend.expressions.util.AutomationFunctions;
import de.swiftbyte.gmc.backend.exception.AutomationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class AutomationExpressionEngine {

    private static final TemplateParserContext TEMPLATE = new TemplateParserContext("{", "}");
    // Detect only braced expressions that start with known symbols to avoid matching JSON blocks
    private static final Pattern BRACED_EXPR = Pattern.compile(
            "(?<!\\{)\\{\\s*(automation\\b|run\\b|step\\b|targets\\b|outputs\\b|teamName\\b|nowIso\\b|epoch\\b|date\\b|timeHms\\b)[^{}]*}(?!})");

    private final ExpressionParser parser = new SpelExpressionParser();
    private final MongoTemplate mongoTemplate;

    public AutomationExpressionEngine(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String render(String template, Automation automation, AutomationRun run) {
        if (template == null) return null;
        if (!containsTemplate(template)) return template;
        return renderField("template", template, automation, run);
    }

    public String renderField(String fieldName, String template, Automation automation, AutomationRun run) {
        if (template == null) return null;
        if (!containsTemplate(template)) return template;
        String normalized = normalizeToTemplate(template);
        try {
            StandardEvaluationContext ctx = buildContext(automation, run);
            Expression expr = parser.parseExpression(normalized, TEMPLATE);
            Object value = expr.getValue(ctx, String.class);
            return value == null ? "" : String.valueOf(value);
        } catch (EvaluationException | IllegalArgumentException e) {
            log.warn("Automation expression failed in {}: {}", fieldName, summarize(e));
            throw new AutomationException("expression_error");
        } catch (Exception e) {
            log.warn("Automation expression failed in {}: {}", fieldName, summarize(e));
            throw new AutomationException("expression_error");
        }
    }

    private String summarize(Throwable e) {
        if (e == null) return "expression_error";
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return e.getClass().getSimpleName();
        // Keep it concise: first 200 chars, strip newlines
        msg = msg.replaceAll("\n", " ").trim();
        if (msg.length() > 200) msg = msg.substring(0, 200) + "...";
        return msg;
    }

    private boolean containsTemplate(String s) {
        Matcher m = BRACED_EXPR.matcher(s);
        return m.find();
    }

    private String normalizeToTemplate(String s) {
        // The engine now uses single‑brace templates directly: { ... }
        // No conversion required.
        return s;
    }

    private StandardEvaluationContext buildContext(Automation automation, AutomationRun run) {
        AutomationEvalContext root = new AutomationEvalContext(automation, run, mongoTemplate);
        StandardEvaluationContext ctx = new StandardEvaluationContext(root);
        // Register utility functions
        registerFunctions(ctx, AutomationFunctions.class,
                "join", "upper", "lower", "trim", "nowIso", "epoch", "formatDate", "min", "max", "avg", "sum");
        return ctx;
    }

    private void registerFunctions(StandardEvaluationContext ctx, Class<?> clazz, String... names) {
        Set<String> done = new HashSet<>();
        for (String n : names) {
            if (!done.add(n)) continue;
            try {
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(n)) {
                        ctx.registerFunction(n, m);
                    }
                }
            } catch (Exception ignored) { }
        }
    }
}
