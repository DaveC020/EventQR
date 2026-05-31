package com.thedavelopers.eventqr.features.uploads.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedavelopers.eventqr.features.uploads.model.dto.StoredFileResponse;
import com.thedavelopers.eventqr.shared.exceptions.BadRequestException;
import com.thedavelopers.eventqr.shared.exceptions.ResourceNotFoundException;

@Service
public class FileStorageService {

    private static final String CONTENT_FILE_NAME = "content.bin";
    private static final String METADATA_FILE_NAME = "metadata.properties";
    private final Path storageRoot = Paths.get(System.getProperty("user.dir"), "storage", "uploads");

    public StoredFileResponse store(UUID ownerId, String purpose, MultipartFile file) {
        validateFile(file);
        try {
            UUID fileId = UUID.randomUUID();
            Instant storedAt = Instant.now();
            Path fileDirectory = resolveFileDirectory(fileId);
            Files.createDirectories(fileDirectory);
            Files.copy(file.getInputStream(), fileDirectory.resolve(CONTENT_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
            writeMetadata(fileDirectory.resolve(METADATA_FILE_NAME), ownerId, purpose, file.getOriginalFilename(), file.getContentType(), storedAt);
            return readRecord(fileId).toResponse("STORED");
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read uploaded file");
        }
    }

    public StoredFileResponse find(UUID fileId) {
        return require(fileId).toResponse("AVAILABLE");
    }

    public StoredFileContent readContent(UUID fileId) {
        FileStorageRecord record = require(fileId);
        return new StoredFileContent(record.content, record.contentType);
    }

    public StoredFileResponse delete(UUID fileId) {
        FileStorageRecord record = require(fileId);
        deleteDirectory(record.fileDirectory);
        return record.toResponse("DELETED");
    }

    public String buildContentPath(UUID fileId) {
        return "files/" + fileId + "/content";
    }

    private FileStorageRecord require(UUID fileId) {
        return readRecord(fileId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
    }

    private Path resolveFileDirectory(UUID fileId) {
        return storageRoot.resolve(fileId.toString());
    }

    private void writeMetadata(Path metadataFile, UUID ownerId, String purpose, String fileName, String contentType,
                               Instant storedAt) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("ownerId", ownerId == null ? "" : ownerId.toString());
        properties.setProperty("purpose", purpose == null ? "" : purpose);
        properties.setProperty("fileName", fileName == null ? "" : fileName);
        properties.setProperty("contentType", contentType == null ? "" : contentType);
        properties.setProperty("storedAt", storedAt.toString());
        try (var output = Files.newOutputStream(metadataFile)) {
            properties.store(output, "EventQR upload metadata");
        }
    }

    private FileStorageRecord readRecord(UUID fileId) {
        try {
            Path fileDirectory = resolveFileDirectory(fileId);
            Path contentFile = fileDirectory.resolve(CONTENT_FILE_NAME);
            Path metadataFile = fileDirectory.resolve(METADATA_FILE_NAME);
            if (!Files.exists(contentFile) || !Files.exists(metadataFile)) {
                throw new ResourceNotFoundException("File not found: " + fileId);
            }

            Properties properties = new Properties();
            try (var input = Files.newInputStream(metadataFile)) {
                properties.load(input);
            }

            byte[] content = Files.readAllBytes(contentFile);
            return new FileStorageRecord(
                    fileId,
                    parseUuid(properties.getProperty("ownerId")),
                    blankToNull(properties.getProperty("purpose")),
                    blankToNull(properties.getProperty("fileName")),
                    blankToNull(properties.getProperty("contentType")),
                    content,
                    parseInstant(properties.getProperty("storedAt")),
                    fileDirectory);
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read stored file");
        }
    }

    private UUID parseUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value.trim());
    }

    private Instant parseInstant(String value) {
        return value == null || value.isBlank() ? Instant.now() : Instant.parse(value.trim());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void deleteDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        } catch (IOException exception) {
            throw new BadRequestException("Unable to delete stored file");
        }
    }

    private record FileStorageRecord(UUID fileId, UUID ownerId, String purpose, String fileName,
                                     String contentType, byte[] content, Instant storedAt, Path fileDirectory) {
        StoredFileResponse toResponse(String status) {
            return new StoredFileResponse(fileId, ownerId, purpose, fileName,
                    contentType, content == null ? 0 : content.length, status, storedAt, encode(content));
        }
    }

    @SuppressWarnings("unused")
    private static String encode(byte[] content) {
        return Base64.getEncoder().encodeToString(content == null ? new byte[0] : content);
    }

    public record StoredFileContent(byte[] content, String contentType) {
    }
}
