package com.pavlent1yy.gradinator.service.parser;

import com.pavlent1yy.gradinator.config.StorageContext;
import com.pavlent1yy.gradinator.entity.ScheduleFile;
import com.pavlent1yy.gradinator.repository.ScheduleFileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private final ExcelLayoutScanner scanner;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @PostConstruct
    private void fileUpload() {
        Path storageDir = storageContext.getStorageDir();

        try {
            files.clear();

            try (Stream<Path> stream = Files.list(storageDir)) {
                stream.filter(Files::isRegularFile)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".xlsx"))
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
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
        Path target = storageContext.resolve(filename);
        String localHash = Files.exists(target) ? sha256(Files.readAllBytes(target)) : null;
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(sourceUrl + filename))
                .timeout(Duration.ofSeconds(10));
        boolean conditional = localHash != null && localHash.equals(record.getHash()) && record.getEtag() != null;
        if (conditional) {
            request.header("If-None-Match", record.getEtag());
        }

        HttpResponse<byte[]> response = client.send(request.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        if (conditional && response.statusCode() == 304) return false;
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " при загрузке " + filename);
        }
        byte[] content = response.body();

        String newHash = sha256(content);
        boolean changed = !newHash.equals(localHash);
        if (changed) {
            validateWorkbook(content);
            Path temporary = Files.createTempFile(target.getParent(), ".schedule-", ".tmp");
            try {
                Files.write(temporary, content);
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
            record.setUpdatedAt(LocalDateTime.now());
            log.info("🔵 Файл {} обновлён, новый hash {}", filename, newHash);
        }

        record.setEtag(response.headers().firstValue("ETag").orElse(null));
        record.setSize((long) content.length);
        record.setHash(newHash);
        try {
            fileRepository.save(record);
        } catch (RuntimeException e) {
            log.error("⭕ Не удалось сохранить метаданные {}", filename, e);
        }
        return changed;
    }

    private void validateWorkbook(byte[] content) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            boolean hasGroups = false;
            for (var sheet : workbook) {
                var groups = scanner.scan(sheet);
                if (groups.stream().anyMatch(group -> group.getDays().size() != 6)) {
                    throw new IOException("В книге есть группы с неполным расписанием");
                }
                hasGroups |= !groups.isEmpty();
            }
            if (!hasGroups) throw new IOException("В книге нет расписания групп");
        }
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
