package de.swiftbyte.gmc.backend.exception;

public class SettingsException extends GmcException {

    public SettingsException(String message) {
        super("settings." + message);
    }
}

