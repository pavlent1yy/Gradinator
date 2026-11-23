package com.pavlent1yy.gradinator.service.parser;

import com.pavlent1yy.gradinator.config.StorageContext;
import com.pavlent1yy.gradinator.entity.ScheduleFile;
import com.pavlent1yy.gradinator.repository.ScheduleFileRepository;
import com.pavlent1yy.gradinator.support.SheetBuilder;
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

        service = new ExcelFileSyncService(storage, fileRepository, new ExcelLayoutScanner());
        ReflectionTestUtils.setField(service, "sourceUrl", "https://college.test/rasp/");
        ReflectionTestUtils.setField(service, "client", client);
        ReflectionTestUtils.invokeMethod(service, "fileUpload");

        when(fileRepository.findByFilename(any())).thenReturn(Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private void get(int status, String etag, byte[] body) throws Exception {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of("ETag", List.of(etag)), (a, b) -> true));
        when(response.body()).thenReturn(body);
        doReturn(response).when(client).send(argThat(r -> r != null && r.method().equals("GET")), any());
    }

    private byte[] workbook() throws Exception {
        return Files.readAllBytes(new SheetBuilder().fullWeek("ИС1-12", "История")
                .writeTo(storageDir.resolve("candidate")));
    }

    private ScheduleFile lastSaved() {
        ArgumentCaptor<ScheduleFile> captor = ArgumentCaptor.forClass(ScheduleFile.class);
        verify(fileRepository).save(captor.capture());
        return captor.getValue();
    }

    private static String sha256(byte[] content) throws Exception {
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(content);
        return java.util.HexFormat.of().formatHex(hash);
    }

    @Test
    void downloadsChangedWorkbookAndStoresMetadata() throws Exception {
        byte[] content = workbook();
        get(200, "\"e1\"", content);

        assertThat(service.syncAll()).isTrue();

        assertThat(Files.readAllBytes(storageDir.resolve("oit.xlsx"))).isEqualTo(content);
        ScheduleFile saved = lastSaved();
        assertThat(saved.getFilename()).isEqualTo("oit.xlsx");
        assertThat(saved.getEtag()).isEqualTo("\"e1\"");
        assertThat(saved.getSize()).isEqualTo((long) content.length);
        assertThat(saved.getHash()).isEqualTo(sha256(content));
        assertThat(saved.getUpdatedAt()).isNotNull();

        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(requests.capture(), any());
        assertThat(requests.getValue().uri().toString()).isEqualTo("https://college.test/rasp/oit.xlsx");
    }

    @Test
    void usesEtagOnlyWhenLocalFileMatchesStoredHash() throws Exception {
        byte[] local = Files.readAllBytes(storageDir.resolve("oit.xlsx"));
        ScheduleFile known = ScheduleFile.builder().id(1L).filename("oit.xlsx")
                .etag("\"e1\"").hash(sha256(local)).build();
        when(fileRepository.findByFilename("oit.xlsx")).thenReturn(Optional.of(known));
        get(304, "\"e1\"", new byte[0]);

        assertThat(service.syncAll()).isFalse();

        verify(client).send(argThat(r -> r.headers().firstValue("If-None-Match").orElse("").equals("\"e1\"")), any());
        verify(fileRepository, never()).save(any());
    }

    @Test
    void repairsLocalFileEvenIfDownloadedHashMatchesStoredHash() throws Exception {
        byte[] content = workbook();
        ScheduleFile known = ScheduleFile.builder().id(1L).filename("oit.xlsx")
                .etag("\"e1\"").hash(sha256(content)).build();
        when(fileRepository.findByFilename("oit.xlsx")).thenReturn(Optional.of(known));
        get(200, "\"e1\"", content);

        assertThat(service.syncAll()).isTrue();

        verify(client).send(argThat(r -> r.headers().firstValue("If-None-Match").isEmpty()), any());
        assertThat(Files.readAllBytes(storageDir.resolve("oit.xlsx"))).isEqualTo(content);
    }

    @Test
    void sameContentUnderNewEtagOnlyUpdatesMetadata() throws Exception {
        byte[] content = workbook();
        Files.write(storageDir.resolve("oit.xlsx"), content);
        get(200, "\"e2\"", content);

        assertThat(service.syncAll()).isFalse();
        assertThat(lastSaved().getEtag()).isEqualTo("\"e2\"");
        assertThat(lastSaved().getUpdatedAt()).isNull();
    }

    @Test
    void keepsLocalWorkbookWhenServerReturnsHtmlAsSuccess() throws Exception {
        get(200, "\"e1\"", "<html>Maintenance</html>".getBytes(StandardCharsets.UTF_8));

        assertThat(service.syncAll()).isFalse();

        verify(fileRepository, never()).save(any());
        assertThat(Files.readString(storageDir.resolve("oit.xlsx"))).isEqualTo("old");
    }

    @Test
    void httpErrorIsSwallowedPerFile() throws Exception {
        get(500, "\"e1\"", new byte[0]);

        assertThat(service.syncAll()).isFalse();

        verify(fileRepository, never()).save(any());
        assertThat(Files.readString(storageDir.resolve("oit.xlsx"))).isEqualTo("old");
    }

    @Test
    void onlyXlsxFilesAreSynced() throws Exception {
        Files.writeString(storageDir.resolve("ignore.txt"), "ignore");
        ReflectionTestUtils.invokeMethod(service, "fileUpload");
        get(500, "\"e1\"", new byte[0]);

        service.syncAll();

        verify(fileRepository).findByFilename("oit.xlsx");
        verify(fileRepository, never()).findByFilename("subdir");
        verify(fileRepository, never()).findByFilename("ignore.txt");
    }

    @Test
    void failsOnMissingStorageDir() {
        StorageContext storage = mock(StorageContext.class);
        when(storage.getStorageDir()).thenReturn(storageDir.resolve("missing"));
        ExcelFileSyncService broken = new ExcelFileSyncService(storage, fileRepository, new ExcelLayoutScanner());

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(broken, "fileUpload"))
                .hasMessageContaining("Не удалось загрузить список файлов");
    }
}
