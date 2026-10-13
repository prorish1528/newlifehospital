package com.newlife.hospital.service;

import com.newlife.hospital.config.AppProperties;
import com.newlife.hospital.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores doctor photos on disk under random names. The file type is decided from the file's magic bytes, never
 * from the client-supplied name or content type, and served names are strictly pattern-checked to prevent
 * path traversal.
 */
@Service
public class FileStorageService {

    public static final String DOCTOR_PHOTO_URL_PREFIX = "/api/public/files/doctors/";
    private static final Pattern SAFE_NAME = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    public record StoredFile(Path path, String contentType) {}

    private final Path doctorDir;

    public FileStorageService(AppProperties props) {
        this.doctorDir = Path.of(props.uploadDir()).toAbsolutePath().normalize().resolve("doctors");
        try {
            Files.createDirectories(doctorDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload directory " + doctorDir, e);
        }
    }

    /** Saves the photo and returns its public URL. */
    public String storeDoctorPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Choose an image to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("Image must be 2 MB or smaller.");
        }
        try {
            byte[] head = new byte[12];
            int read;
            try (InputStream in = file.getInputStream()) {
                read = in.readNBytes(head, 0, head.length);
            }
            String ext = detectExtension(head, read)
                    .orElseThrow(() -> ApiException.badRequest("Only JPEG, PNG or WebP images are allowed."));
            String name = UUID.randomUUID() + "." + ext;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, doctorDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return DOCTOR_PHOTO_URL_PREFIX + name;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store image", e);
        }
    }

    public Optional<StoredFile> loadDoctorPhoto(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            return Optional.empty();
        }
        Path path = doctorDir.resolve(name).normalize();
        if (!path.startsWith(doctorDir) || !Files.isRegularFile(path)) {
            return Optional.empty();
        }
        String type = switch (name.substring(name.lastIndexOf('.') + 1)) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
        return Optional.of(new StoredFile(path, type));
    }

    public void deleteDoctorPhoto(String url) {
        if (url == null || !url.startsWith(DOCTOR_PHOTO_URL_PREFIX)) {
            return;
        }
        loadDoctorPhoto(url.substring(DOCTOR_PHOTO_URL_PREFIX.length())).ifPresent(f -> {
            try {
                Files.deleteIfExists(f.path());
            } catch (IOException ignored) {
                // An orphaned file is harmless; it is no longer referenced.
            }
        });
    }

    static Optional<String> detectExtension(byte[] h, int len) {
        if (len >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return Optional.of("jpg");
        }
        if (len >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return Optional.of("png");
        }
        if (len >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return Optional.of("webp");
        }
        return Optional.empty();
    }
}
