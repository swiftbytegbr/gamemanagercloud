package de.swiftbyte.gmc.backend.exception;

public class VerificationException extends GmcException {

    public VerificationException(String message) {
        super("verification." + message);
    }

    public VerificationException() {
    }
}
