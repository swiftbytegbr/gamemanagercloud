package de.swiftbyte.gmc.backend.exception;

public class TeamException extends GmcException {

    public TeamException(String message) {
        super("team." + message);
    }
}
