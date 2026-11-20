package de.swiftbyte.gmc.backend.exception;

public class ServerException extends GmcException {

    public ServerException(String message) {
        super("server." + message);
    }
}
