package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.ScheduleFile;
import com.pavlent1yy.gradinator.repository.ScheduleFileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelFileSyncService {

    @Value("${api.file-source-url}")
    private String sourceUrl;
    private final List<String> files;

    @PostConstruct
    private void fileUpload(){
        try {
            Path dir = Paths.get(Objects.requireNonNull(getClass().getClassLoader()
                            .getResource("scheduleFiles"))
                    .toURI());

            try (Stream<Path> stream = Files.list(dir)) {
                files.clear();

                stream.map(Path::getFileName)
                        .map(Path::toString)
                        .forEach(files::add);
            }

            System.out.println(files);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Не удалось загрузить список файлов", e);
        }
    }



    private static final Path STORAGE_DIR = Path.of("src/main/resources/scheduleFiles");

    private final ScheduleFileRepository fileRepository;
    private final HttpClient client = HttpClient.newHttpClient();

    // true, если хотя бы один файл реально обновился
    public boolean syncAll() {
        log.info("🔵Синхронизация Excel файлов...");
        boolean anyChanged = false;
        for (String filename : files) {
            try {
                anyChanged |= syncOne(filename);
            } catch (Exception e) {
                log.error("⭕Не удалось синхронизировать {}", filename, e);
            }
        }
        return anyChanged;
    }

    private boolean syncOne(String filename) throws Exception {
        log.debug("🐜Синхронизация файла {}",filename);
        ScheduleFile record = fileRepository.findByFilename(filename)
                .orElseGet(() -> ScheduleFile.builder().filename(filename).build());

        HttpRequest head = HttpRequest.newBuilder(URI.create(sourceUrl + filename))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<Void> headResponse = client.send(head, HttpResponse.BodyHandlers.discarding());

        String remoteEtag = headResponse.headers().firstValue("ETag").orElse(null);
        long remoteSize = headResponse.headers().firstValueAsLong("Content-Length").orElse(-1);

        boolean etagChanged = remoteEtag != null && !remoteEtag.equals(record.getEtag());
        boolean sizeChanged = remoteSize >= 0 && !Long.valueOf(remoteSize).equals(record.getSize());

        if (!etagChanged && !sizeChanged && record.getId() != null) {
            return false; // метаданные не поменялись - файл не трогаем
        }

        HttpRequest get = HttpRequest.newBuilder(URI.create(sourceUrl + filename)).GET().build();
        HttpResponse<byte[]> response = client.send(get, HttpResponse.BodyHandlers.ofByteArray());
        byte[] content = response.body();

        String newHash = sha256(content);

        if (newHash.equals(record.getHash())) {
            // метаданные соврали / сайт тронул только заголовки - контент тот же
            record.setEtag(remoteEtag);
            record.setSize(remoteSize);
            fileRepository.save(record);
            return false;
        }

        Files.write(STORAGE_DIR.resolve(filename), content,
                StandardCopyOption.REPLACE_EXISTING == null ?
                        new java.nio.file.OpenOption[0] : new java.nio.file.OpenOption[0]);
        Files.write(STORAGE_DIR.resolve(filename), content);

        record.setEtag(remoteEtag);
        record.setSize((long) content.length);
        record.setHash(newHash);
        record.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(record);

        log.info("🔵Файл {} обновлён, новый hash {}", filename, newHash);
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