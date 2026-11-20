package de.swiftbyte.gmc.backend.exception;

public class NodeException extends GmcException {

    public NodeException(String message) {
        super("node." + message);
    }
}
