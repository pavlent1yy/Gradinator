package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.config.StorageContext;
import com.pavlent1yy.gradinator.entity.ScheduleFile;
import com.pavlent1yy.gradinator.repository.ScheduleFileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelFileSyncService {

    @Value("${api.file-source-url}")
    private String sourceUrl;

    private final List<String> files = new ArrayList<>();

    private final StorageContext storageContext;
    private final ScheduleFileRepository fileRepository;

    private final HttpClient client = HttpClient.newHttpClient();

    @PostConstruct
    private void fileUpload() {
        Path storageDir = storageContext.getStorageDir();

        try {
            files.clear();

            try (Stream<Path> stream = Files.list(storageDir)) {
                stream.filter(Files::isRegularFile)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .forEach(files::add);
            }

            log.debug("📊 Найдено файлов: {}", files);

        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить список файлов из " + storageDir, e);
        }
    }

    // true, если хотя бы один файл реально обновился
    public boolean syncAll() {

        log.info("🔵 Синхронизация Excel файлов...");
        boolean anyChanged = false;

        for (String filename : files) {
            try {
                anyChanged |= syncOne(filename);
            } catch (Exception e) {
                log.error("⭕ Не удалось синхронизировать {}", filename, e);
            }
        }

        return anyChanged;
    }

    private boolean syncOne(String filename) throws Exception {

        log.debug("🐜 Синхронизация файла {}", filename);
        ScheduleFile record = fileRepository
                .findByFilename(filename).orElseGet(() -> ScheduleFile.builder().filename(filename).build());
        String url = sourceUrl + filename;
        HttpRequest head = HttpRequest.newBuilder(
                        URI.create(url)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<Void> headResponse = client.send(head, HttpResponse.BodyHandlers.discarding());
        String remoteEtag = headResponse.headers().firstValue("ETag").orElse(null);
        long remoteSize = headResponse.headers().firstValueAsLong("Content-Length").orElse(-1);
        boolean etagChanged = remoteEtag != null && !remoteEtag.equals(record.getEtag());
        boolean sizeChanged = remoteSize >= 0 && !Long.valueOf(remoteSize).equals(record.getSize());

        if (!etagChanged && !sizeChanged && record.getId() != null) {
            return false;
        }

        HttpRequest get = HttpRequest.newBuilder(URI.create(url)).GET().build();

        HttpResponse<byte[]> response = client.send(get, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " при загрузке " + filename);
        }
        byte[] content = response.body();

        String newHash = sha256(content);
        if (newHash.equals(record.getHash())) {

            record.setEtag(remoteEtag);
            record.setSize((long) content.length);

            fileRepository.save(record);

            return false;
        }

        Path target = storageContext.resolve(filename);

        Files.write(
                target,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        record.setEtag(remoteEtag);
        record.setSize((long) content.length);
        record.setHash(newHash);
        record.setUpdatedAt(LocalDateTime.now());

        fileRepository.save(record);

        log.info("🔵 Файл {} обновлён, новый hash {}", filename, newHash);
        return true;
    }

    private String sha256(byte[] content) throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content);
        
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }
}
