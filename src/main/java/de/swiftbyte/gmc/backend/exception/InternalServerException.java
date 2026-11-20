package de.swiftbyte.gmc.backend.exception;

/**
 * Internal server exception using the base GmcException fields for errorKey and customMessage.
 */
public class InternalServerException extends GmcException {

    public InternalServerException(String customMessage) {
        super("general.internal_server_error", customMessage);
    }

    public InternalServerException(String errorKey, String customMessage) {
        super(errorKey, customMessage);
    }

    public InternalServerException(String errorKey, String customMessage, Throwable cause) {
        super(errorKey, customMessage, cause);
    }
}
