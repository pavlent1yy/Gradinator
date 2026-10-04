package com.pavlent1yy.gradinator.service.parser;

import com.pavlent1yy.gradinator.config.StorageContext;
import com.pavlent1yy.gradinator.entity.ScheduleFile;
import com.pavlent1yy.gradinator.repository.ScheduleFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ExcelFileSyncServiceTest {

    @TempDir
    Path storageDir;

    private final ScheduleFileRepository fileRepository = mock(ScheduleFileRepository.class);
    private final HttpClient client = mock(HttpClient.class);
    private ExcelFileSyncService service;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(storageDir.resolve("oit.xlsx"), "old");
        Files.createDirectory(storageDir.resolve("subdir"));

        StorageContext storage = new StorageContext();
        ReflectionTestUtils.setField(storage, "storageDirPath", storageDir.toString());
        storage.init();

        service = new ExcelFileSyncService(storage, fileRepository);
        ReflectionTestUtils.setField(service, "sourceUrl", "https://college.test/rasp/");
        ReflectionTestUtils.setField(service, "client", client);
        ReflectionTestUtils.invokeMethod(service, "fileUpload");

        when(fileRepository.findByFilename(any())).thenReturn(Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private void head(String etag, long size) throws Exception {
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.headers()).thenReturn(HttpHeaders.of(
                Map.of("ETag", List.of(etag), "Content-Length", List.of(String.valueOf(size))),
                (a, b) -> true));
        doReturn(response).when(client).send(argThat(r -> r != null && r.method().equals("HEAD")), any());
    }

    @SuppressWarnings("unchecked")
    private void get(int status, String body) throws Exception {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        doReturn(response).when(client).send(argThat(r -> r != null && r.method().equals("GET")), any());
    }

    private ScheduleFile lastSaved() {
        ArgumentCaptor<ScheduleFile> captor = ArgumentCaptor.forClass(ScheduleFile.class);
        verify(fileRepository).save(captor.capture());
        return captor.getValue();
    }

    private static String sha256(String s) throws Exception {
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(hash);
    }

    @Test
    void downloadsNewFileAndStoresMetadata() throws Exception {
        head("\"e1\"", 3);
        get(200, "new");

        assertThat(service.syncAll()).isTrue();

        assertThat(Files.readString(storageDir.resolve("oit.xlsx"))).isEqualTo("new");
        ScheduleFile saved = lastSaved();
        assertThat(saved.getFilename()).isEqualTo("oit.xlsx");
        assertThat(saved.getEtag()).isEqualTo("\"e1\"");
        assertThat(saved.getSize()).isEqualTo(3L);
        assertThat(saved.getHash()).isEqualTo(sha256("new"));
        assertThat(saved.getUpdatedAt()).isNotNull();

        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(2)).send(requests.capture(), any());
        assertThat(requests.getAllValues()).allSatisfy(r ->
                assertThat(r.uri().toString()).isEqualTo("https://college.test/rasp/oit.xlsx"));
    }

    @Test
    void skipsDownloadWhenEtagAndSizeUnchanged() throws Exception {
        ScheduleFile known = ScheduleFile.builder().id(1L).filename("oit.xlsx").etag("\"e1\"").size(3L).build();
        when(fileRepository.findByFilename("oit.xlsx")).thenReturn(Optional.of(known));
        head("\"e1\"", 3);

        assertThat(service.syncAll()).isFalse();

        verify(client, times(1)).send(any(), any());
        verify(fileRepository, never()).save(any());
        assertThat(Files.readString(storageDir.resolve("oit.xlsx"))).isEqualTo("old");
    }

    @Test
    void sameContentUnderNewEtagOnlyUpdatesMetadata() throws Exception {
        ScheduleFile known = ScheduleFile.builder().id(1L).filename("oit.xlsx")
                .etag("\"e1\"").size(3L).hash(sha256("new")).build();
        when(fileRepository.findByFilename("oit.xlsx")).thenReturn(Optional.of(known));
        head("\"e2\"", 3);
        get(200, "new");

        assertThat(service.syncAll()).isFalse();

        assertThat(lastSaved().getEtag()).isEqualTo("\"e2\"");
        assertThat(Files.readString(storageDir.resolve("oit.xlsx"))).isEqualTo("old");
    }

    @Test
    void httpErrorIsSwallowedPerFile() throws Exception {
        head("\"e1\"", 3);
        get(500, "");

        assertThat(service.syncAll()).isFalse();

        verify(fileRepository, never()).save(any());
        assertThat(Files.readString(storageDir.resolve("oit.xlsx"))).isEqualTo("old");
    }

    @Test
    void onlyRegularFilesAreSynced() throws Exception {
        head("\"e1\"", 3);
        get(200, "new");

        service.syncAll();

        verify(fileRepository).findByFilename("oit.xlsx");
        verify(fileRepository, never()).findByFilename("subdir");
    }

    @Test
    void failsOnMissingStorageDir() {
        StorageContext storage = mock(StorageContext.class);
        when(storage.getStorageDir()).thenReturn(storageDir.resolve("missing"));
        ExcelFileSyncService broken = new ExcelFileSyncService(storage, fileRepository);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(broken, "fileUpload"))
                .hasMessageContaining("Не удалось загрузить список файлов");
    }
}
