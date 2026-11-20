package de.swiftbyte.gmc.backend.exception;

public class AuthenticationException extends GmcException {


    public AuthenticationException(String errorKey) {
        super("auth." + errorKey);
    }
}
