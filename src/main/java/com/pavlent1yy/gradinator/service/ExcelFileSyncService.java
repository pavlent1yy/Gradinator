package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.ScheduleFile;
import com.pavlent1yy.gradinator.repository.ScheduleFileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class ExcelFileSyncService {

    private static final String BASE_URL = "https://ygk.edu.yar.ru/pages/rasp/25-26/";

    private static final List<String> FILES = List.of(
            "mmo_2sem.xlsx", "oar_2sem.xlsx", "oep_2sem.xlsx", "oit_2sem.xlsx", "so_2sem.xlsx"
    );

    private static final Path STORAGE_DIR = Path.of("src/main/resources/scheduleFiles");

    private final ScheduleFileRepository fileRepository;
    private final HttpClient client = HttpClient.newHttpClient();

    /** true, если хотя бы один файл реально обновился */
    public boolean syncAll() {
        boolean anyChanged = false;
        for (String filename : FILES) {
            try {
                anyChanged |= syncOne(filename);
            } catch (Exception e) {
                log.error("Не удалось синхронизировать {}", filename, e);
            }
        }
        return anyChanged;
    }

    private boolean syncOne(String filename) throws Exception {
        ScheduleFile record = fileRepository.findByFilename(filename)
                .orElseGet(() -> ScheduleFile.builder().filename(filename).build());

        HttpRequest head = HttpRequest.newBuilder(URI.create(BASE_URL + filename)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<Void> headResponse = client.send(head, HttpResponse.BodyHandlers.discarding());

        String remoteEtag = headResponse.headers().firstValue("ETag").orElse(null);
        long remoteSize = headResponse.headers().firstValueAsLong("Content-Length").orElse(-1);

        boolean etagChanged = remoteEtag != null && !remoteEtag.equals(record.getEtag());
        boolean sizeChanged = remoteSize >= 0 && !Long.valueOf(remoteSize).equals(record.getSize());

        if (!etagChanged && !sizeChanged && record.getId() != null) {
            return false; // метаданные не поменялись — файл не трогаем
        }

        HttpRequest get = HttpRequest.newBuilder(URI.create(BASE_URL + filename)).GET().build();
        HttpResponse<byte[]> response = client.send(get, HttpResponse.BodyHandlers.ofByteArray());
        byte[] content = response.body();

        String newHash = sha256(content);

        if (newHash.equals(record.getHash())) {
            // метаданные соврали / сайт тронул только заголовки — контент тот же
            record.setEtag(remoteEtag);
            record.setSize(remoteSize);
            fileRepository.save(record);
            return false;
        }

        Files.write(STORAGE_DIR.resolve(filename), content, StandardCopyOption.REPLACE_EXISTING == null ? new java.nio.file.OpenOption[0] : new java.nio.file.OpenOption[0]);
        Files.write(STORAGE_DIR.resolve(filename), content);

        record.setEtag(remoteEtag);
        record.setSize((long) content.length);
        record.setHash(newHash);
        record.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(record);

        log.info("Файл {} обновлён, новый hash {}", filename, newHash);
        return true;
    }

    private String sha256(byte[] content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}