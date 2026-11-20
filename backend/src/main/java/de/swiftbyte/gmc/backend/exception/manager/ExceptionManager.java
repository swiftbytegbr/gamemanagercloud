package de.swiftbyte.gmc.backend.exception.manager;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.NodeDto;
import de.swiftbyte.gmc.backend.dto.UserDto;
import de.swiftbyte.gmc.backend.exception.*;
import de.swiftbyte.gmc.backend.mapper.NodeMapper;
import de.swiftbyte.gmc.backend.mapper.UserMapper;
import de.swiftbyte.gmc.backend.config.message.NodeAuthentication;
import de.swiftbyte.gmc.backend.security.UserAuthentication;
import de.swiftbyte.gmc.backend.security.ClientIpResolver;
import io.sentry.SentryClient;
import io.sentry.context.Context;
import io.sentry.event.BreadcrumbBuilder;
import io.sentry.event.UserBuilder;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.Event;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ExceptionManager {


    @Autowired(required = false)
    private SentryClient sentryClient;

    @Autowired(required = false)
    private UserMapper userMapper;

    @Autowired(required = false)
    private NodeMapper nodeMapper;

    @Autowired
    private ClientIpResolver clientIpResolver;

    // --- Spring Exceptions ---


    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return base(HttpStatus.BAD_REQUEST, "general.bad_request", request);
    }

    

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return base(HttpStatus.METHOD_NOT_ALLOWED, "general.method_not_allowed", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ErrorResponse handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return base(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "general.unsupported_media_type", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameterException(MissingServletRequestParameterException ex, HttpServletRequest request) {
        ErrorResponse resp = base(HttpStatus.BAD_REQUEST, "general.bad_request", request);
        resp.setMessage("The required request parameter '" + ex.getParameterName() + "' is missing.");
        resp.addViolation(new Violation()
                .setType("parameter")
                .setField(ex.getParameterName())
                .setMessage("missing"));
        return resp;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ErrorResponse resp = base(HttpStatus.BAD_REQUEST, "general.bad_request", request);
        String expected = ex.getRequiredType() == null ? "unknown" : ex.getRequiredType().getSimpleName();
        resp.setMessage("The parameter '" + ex.getName() + "' is invalid. Expected type: " + expected + ".");
        resp.addViolation(new Violation()
                .setType("parameter")
                .setField(ex.getName())
                .setMessage("type_mismatch")
                .setRejectedValue(ex.getValue()));
        return resp;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ErrorResponse resp = base(HttpStatus.BAD_REQUEST, "validation.failed", request);

        List<String> phrases = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            String rawMsg = fe.getDefaultMessage();
            String code = fe.getCode();
            String field = fe.getField();
            String phrase = ValidationMessageUtil.toFriendlyPhrase(field, rawMsg, code, fe.getRejectedValue());
            if (phrase != null && !phrase.isBlank()) phrases.add(phrase);
            resp.addViolation(new Violation()
                    .setType("field")
                    .setField(field)
                    .setMessage(ValidationMessageUtil.ensureSentence(phrase))
                    .setCode(code)
                    .setRejectedValue(fe.getRejectedValue()));
        }
        for (ObjectError ge : ex.getBindingResult().getGlobalErrors()) {
            String obj = ge.getObjectName();
            String rawMsg = ge.getDefaultMessage();
            String code = ge.getCode();
            String phrase = ValidationMessageUtil.toFriendlyPhrase(obj, rawMsg, code, null);
            if (phrase != null && !phrase.isBlank()) phrases.add(phrase);
            resp.addViolation(new Violation()
                    .setType("object")
                    .setField(obj)
                    .setMessage(ValidationMessageUtil.ensureSentence(phrase))
                    .setCode(code));
        }
        if (!phrases.isEmpty()) {
            resp.setMessage(ValidationMessageUtil.composeRootMessage(phrases));
        }
        return resp;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        ErrorResponse resp = base(HttpStatus.BAD_REQUEST, "validation.failed", request);
        List<String> phrases = new ArrayList<>();
        ex.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath() == null ? "parameter" : v.getPropertyPath().toString();
            String code = v.getConstraintDescriptor() == null ? null : v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
            String rawMsg = v.getMessage();
            String phrase = ValidationMessageUtil.toFriendlyPhrase(path, rawMsg, code, v.getInvalidValue());
            if (phrase != null && !phrase.isBlank()) phrases.add(phrase);
            resp.addViolation(new Violation()
                    .setType("parameter")
                    .setField(path)
                    .setMessage(ValidationMessageUtil.ensureSentence(phrase))
                    .setCode(code)
                    .setRejectedValue(v.getInvalidValue()));
        });
        if (!phrases.isEmpty()) resp.setMessage(ValidationMessageUtil.composeRootMessage(phrases));
        return resp;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception ex, HttpServletRequest request) {
        log.error("An unexpected error occurred.");
        String traceId = enrichAndSendToSentry(ex, request, "http");
        ErrorResponse resp = base(HttpStatus.INTERNAL_SERVER_ERROR, "general.server_error", request);
        if (traceId != null) resp.setTraceId(traceId);
        return resp;
    }

    // --- Custom Exceptions ---
    
    // Dynamic handler for all GMC domain exceptions. Chooses status from the exception when set,
    // otherwise falls back to a sensible default for the subtype.
    @ExceptionHandler(GmcException.class)
    public ResponseEntity<ErrorResponse> handleGmcException(GmcException ex, HttpServletRequest request) {
        HttpStatus status = ex.getHttpStatus();
        if (status == null) status = defaultStatusFor(ex);

        ErrorResponse resp = base(status, ex.getErrorKey() != null ? ex.getErrorKey() : ex.getMessage(), request);
        if (ex.getCustomMessage() != null) resp.setMessage(ex.getCustomMessage());
        // Preserve legacy forbidden message override
        if (ex instanceof ForbiddenException && ex.getCustomMessage() == null) {
            resp.setMessage("You don't have permission to perform this action.");
        }

        // For server errors, enrich and send to Sentry and include trace id in response
        if (status.is5xxServerError()) {
            String traceId = enrichAndSendToSentry(ex, request, "http");
            if (traceId != null) resp.setTraceId(traceId);
        }

        return ResponseEntity.status(status).body(resp);
    }

    @ExceptionHandler(XlsxParseException.class)
    public ResponseEntity<ErrorResponse> handleXlsxParseException(XlsxParseException ex, HttpServletRequest request) {
        HttpStatus status = ex.getHttpStatus() != null ? ex.getHttpStatus() : HttpStatus.BAD_REQUEST;
        ErrorResponse resp = base(status, "xlsx.parse_exception", request);
        if (ex.getViolations() != null && !ex.getViolations().isEmpty()) {
            for (XlsxViolation xv : ex.getViolations()) {
                Violation v = new Violation()
                        .setType("field")
                        .setField(xv.getColumnName() != null ? xv.getColumnName() : "column_" + xv.getColumnIndex())
                        .setCode(xv.getCode())
                        .setRejectedValue(xv.getRejectedValue())
                        .setMessage("Row " + xv.getRow() + " (index starting at 1)" + (xv.getColumnName() != null ? ", column '" + xv.getColumnName() + "'" : "") + " caused a parsing error.");
                resp.addViolation(v);
            }
        } else {
            Violation v = new Violation()
                    .setType("field")
                    .setField(ex.getFailedColumnName() != null ? ex.getFailedColumnName() : "column_" + ex.getFailedColumnIndex())
                    .setCode(ex.getMessage())
                    .setRejectedValue(ex.getFailedValue())
                    .setMessage("Row " + ex.getFailedRow() + " (index starting at 1)" + (ex.getFailedColumnName() != null ? ", column '" + ex.getFailedColumnName() + "'" : "") + " caused a parsing error.");
            resp.addViolation(v);
        }
        return ResponseEntity.status(status).body(resp);
    }

    // Explicitly handle 404 from Spring's NoResourceFoundException (not a GmcException)
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        return base(HttpStatus.NOT_FOUND, "general.not_found", request);
    }


    @Data
    @Accessors(chain = true)
    public static class ErrorResponse {
        private String errorKey;
        private String message;
        private int status;
        private String path;
        private String method;
        private String timestamp;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String traceId;
        private List<Violation> violations;

        public ErrorResponse() {
            this.violations = new ArrayList<>();
        }

        // Backwards-compatible constructors
        public ErrorResponse(String errorKey) {
            this();
            setErrorKey(errorKey);
        }

        public ErrorResponse(String errorKey, String customMessage) {
            this();
            this.errorKey = errorKey;
            this.message = customMessage;
        }


        public ErrorResponse setErrorKey(String errorKey) {
            this.errorKey = errorKey;
            String msg = ExceptionMessages.ERROR_EXPLANATIONS.getOrDefault(errorKey, errorKey);
            if (msg.equals(errorKey)) log.warn("No explanation for error key '{}'.", errorKey);
            this.message = msg;
            return this;
        }

        public ErrorResponse setStatus(HttpStatus status) {
            this.status = status.value();
            return this;
        }

        public ErrorResponse addViolation(Violation v) {
            this.violations.add(v);
            return this;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class Violation {
        private String type; // field | object | parameter
        private String field;
        private String message;
        private String code;
        private Object rejectedValue;
    }

    public ErrorResponse base(HttpStatus status, String errorKey, HttpServletRequest request) {
        return new ErrorResponse()
                .setStatus(status)
                .setPath(request.getRequestURI())
                .setMethod(request.getMethod())
                .setTimestamp(OffsetDateTime.now().toString())
                .setErrorKey(errorKey);
    }

    private HttpStatus defaultStatusFor(GmcException ex) {
        if (ex instanceof NotFoundException) return HttpStatus.NOT_FOUND;
        if (ex instanceof ForbiddenException) return HttpStatus.FORBIDDEN;
        if (ex instanceof AuthenticationException) return HttpStatus.UNAUTHORIZED;
        if (ex instanceof InternalServerException) return HttpStatus.INTERNAL_SERVER_ERROR;
        // Default for domain validation/business errors
        return HttpStatus.BAD_REQUEST;
    }

    private String enrichAndSendToSentry(Exception ex, HttpServletRequest request, String transportTag) {
        try {
            if (sentryClient == null) {
                ex.printStackTrace();
                return null;
            }
            Context ctx = sentryClient.getContext();

            // Basic HTTP request info extras and breadcrumb
            if (request != null) {
                ctx.addTag("transport", transportTag);
                ctx.addExtra("http.path", request.getRequestURI());
                ctx.addExtra("http.method", request.getMethod());
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null) ctx.addExtra("http.user_agent", userAgent);
                String ip = clientIpResolver != null ? clientIpResolver.resolve(request) : null;
                if (ip != null) ctx.addExtra("http.remote_addr", ip);

                ctx.recordBreadcrumb(new BreadcrumbBuilder()
                        .setCategory("http")
                        .setMessage(request.getMethod() + " " + request.getRequestURI())
                        .build());
            }

            // Attach user or node info, plus map to Sentry user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof UserAuthentication userAuth) {
                Object principal = userAuth.getPrincipal();
                if (principal instanceof User user && userMapper != null) {
                    UserDto userDto = userMapper.toDto(user);
                    ctx.addExtra("user", userDto);
                    ctx.setUser(new UserBuilder()
                            .setId(user.getId())
                            .setEmail(user.getEmail())
                            .build());
                }
            } else if (auth instanceof NodeAuthentication na) {
                Object principal = na.getPrincipal();
                if (principal instanceof Node node && nodeMapper != null) {
                    NodeDto nodeDto = nodeMapper.toDto(node);
                    ctx.addExtra("node", nodeDto); // NodeDto has no secret
                    ctx.setUser(new UserBuilder()
                            .setId(node.getId())
                            .build());
                }
            }

            // Attach GmcException details if present
            if (ex instanceof GmcException ge) {
                if (ge.getErrorKey() != null) ctx.addExtra("gmc.errorKey", ge.getErrorKey());
                if (ge.getCustomMessage() != null) ctx.addExtra("gmc.customMessage", ge.getCustomMessage());
            }

            // Add a raw stacktrace string as a safety net for GlitchTip visibility
            ctx.addExtra("stacktrace", getStackTrace(ex));

            // Build event with explicit exception interface and a known id for response
            UUID eventId = UUID.randomUUID();
            EventBuilder eb = new EventBuilder(eventId)
                    .withLevel(Event.Level.ERROR)
                    .withLogger("gmc-backend")
                    .withMessage(buildTitle(ex, request))
                    .withSentryInterface(new ExceptionInterface(ex));

            // Set a useful culprit if available
            StackTraceElement[] st = ex.getStackTrace();
            if (st != null && st.length > 0) {
                eb.withCulprit(st[0]);
            }
            if (request != null) {
                eb.withTransaction(request.getMethod() + " " + request.getRequestURI());
            }
            sentryClient.sendEvent(eb);
            // Important: clear per-thread Sentry context to avoid leakage when threads are reused
            ctx.clear();
            return eventId.toString();
        } catch (Throwable t) {
            // Ensure we never fail the request path due to Sentry enrichment
            t.printStackTrace();
            return null;
        }
    }

    private static String getStackTrace(Throwable t) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String buildTitle(Exception ex, HttpServletRequest request) {
        String name = ex.getClass().getSimpleName();
        String msg = ex.getMessage();
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (msg != null && !msg.isBlank()) sb.append(": ").append(msg);
        if (request != null) {
            sb.append(" [").append(request.getMethod()).append(" ").append(request.getRequestURI()).append("]");
        }
        return sb.toString();
    }
}
