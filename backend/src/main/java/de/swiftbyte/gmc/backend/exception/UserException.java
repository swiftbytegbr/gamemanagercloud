package de.swiftbyte.gmc.backend.exception;

public class UserException extends GmcException {

    public UserException(String message) {
        super("user." + message);
    }
}
