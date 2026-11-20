package de.swiftbyte.gmc.backend.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class GmcException extends RuntimeException {

    HttpStatus httpStatus;

    // A stable key/code representing the error (e.g. "auth.invalid_node_auth")
    private String errorKey;

    // Optional custom message to surface (e.g. operator-provided detail)
    private String customMessage;

    public GmcException() { super(); }

    public GmcException(String message) {
        super(message);
        // Backwards-compat: treat provided message as the error key
        this.errorKey = message;
    }

    public GmcException(String message, Throwable cause) {
        super(message, cause);
        this.errorKey = message;
    }

    public GmcException(Throwable cause) { super(cause); }

    protected GmcException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorKey = message;
    }

    // New convenience constructors to explicitly supply key and optional custom message
    public GmcException(String errorKey, String customMessage) {
        super(errorKey);
        this.errorKey = errorKey;
        this.customMessage = customMessage;
    }

    public GmcException(String errorKey, String customMessage, Throwable cause) {
        super(errorKey, cause);
        this.errorKey = errorKey;
        this.customMessage = customMessage;
    }
}
