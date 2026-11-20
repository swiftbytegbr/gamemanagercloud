package de.swiftbyte.gmc.backend.exception;

public class BadRequestException extends GmcException {

    public BadRequestException() {
        super();
    }

    public BadRequestException(String message) {
        super(message);
    }
}
