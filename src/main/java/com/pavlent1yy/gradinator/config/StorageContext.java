package com.pavlent1yy.gradinator.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Getter
@Slf4j
public class StorageContext {

    @Value("${api.storage-dir-path}")
    private String storageDirPath;

    private Path storageDir;

    @PostConstruct
    public void init() {
        try {
            storageDir = Path.of(storageDirPath)
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(storageDir);
            log.info("📁 File source directory: {}", storageDir);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Не удалось инициализировать storage: " + storageDirPath, e);
        }
    }

    public Path resolve(String filename) {
        return storageDir.resolve(filename);
    }
}
