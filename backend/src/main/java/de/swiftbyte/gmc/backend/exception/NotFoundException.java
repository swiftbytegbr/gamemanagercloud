package de.swiftbyte.gmc.backend.exception;

public class NotFoundException extends GmcException {

    public NotFoundException() {
        super("general.not_found");
    }
}
