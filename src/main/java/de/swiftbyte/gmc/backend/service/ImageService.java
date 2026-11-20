package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.domain.model.Image;
import de.swiftbyte.gmc.backend.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class ImageService {

    @Autowired
    private ImageRepository imageRepository;

    public Optional<Image> findById(String id) {
        return imageRepository.findById(id);
    }

    public void deleteById(String id) {
        if (id == null || id.isBlank()) return;
        imageRepository.deleteById(id);
    }

    /**
     * Stores an image provided as base64 (optionally prefixed by data URL), returns new image id.
     */
    public String saveBase64(String base64) {
        if (base64 == null) return null;
        String raw = normalizeBase64(base64);
        byte[] bytes = Base64.getMimeDecoder().decode(raw);
        return saveBytes(bytes);
    }

    /**
     * Stores an image provided as raw bytes, returns new image id.
     */
    public String saveBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        String contentType = detectContentType(bytes);
        String format = detectFormat(bytes);
        Image img = new Image();
        img.setContentType(contentType);
        img.setFormat(format);
        img.setData(bytes);
        img.setCreatedAt(Instant.now());
        img = imageRepository.save(img);
        return img.getId();
    }

    public static String normalizeBase64(String value) {
        String raw = value.trim();
        int comma = raw.indexOf(',');
        if (raw.regionMatches(true, 0, "data:", 0, 5) && comma > 0) {
            raw = raw.substring(comma + 1);
        }
        return raw.replaceAll("\\s+", "");
    }

    private static String detectFormat(byte[] bytes) {
        // PNG
        if (bytes.length > 4 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "png";
        }
        // JPEG
        if (bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "jpg";
        }
        // GIF
        if (bytes.length > 3 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "gif";
        }
        // WEBP (RIFF....WEBP)
        if (bytes.length > 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return "bin";
    }

    private static String detectContentType(byte[] bytes) {
        return switch (detectFormat(bytes)) {
            case "png" -> "image/png";
            case "jpg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}

