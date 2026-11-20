package de.swiftbyte.gmc.backend.config;

/**
 * Centralized upload and payload size limits used across the backend.
 */
public final class UploadLimits {

    private UploadLimits() {}

    // Single max size for INI parsing and profile uploads: 5 MB
    public static final int MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
}

