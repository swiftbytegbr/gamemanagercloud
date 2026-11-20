package de.swiftbyte.gmc.backend.exception;

import de.swiftbyte.gmc.backend.domain.enums.Permission;

public class ForbiddenException extends GmcException {

    public ForbiddenException(Permission permission) {
        super("missingPermission." + permission.name());
    }

    public ForbiddenException(String message) {
        super("missingPermission." + message);
    }
}
