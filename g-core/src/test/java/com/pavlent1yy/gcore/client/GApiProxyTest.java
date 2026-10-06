package com.pavlent1yy.gcore.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GApiProxyTest {

    private static final String BASE = "http://g-api.test";

    private MockRestServiceServer server;
    private GApiProxy proxy;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        proxy = new GApiProxy(builder.build());
    }

    private static Map<String, String[]> params(String... pairs) {
        Map<String, String[]> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], new String[]{pairs[i + 1]});
        }
        return map;
    }

    private static String text(ResponseEntity<byte[]> response) {
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }

    @Test
    void forwardsPathAndEncodesQueryStrictly() {
        server.expect(once(), requestTo(BASE + "/api/schedule?date=2026-09-28&group=%D0%98%D0%A11-43%2B%26x"))
                .andRespond(withSuccess("{\"group\":\"ИС1-43\"}", MediaType.APPLICATION_JSON));

        ResponseEntity<byte[]> response = proxy.get("/schedule", params("group", "ИС1-43+&x", "date", "2026-09-28"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(text(response)).contains("ИС1-43");
        server.verify();
    }

    @Test
    void passesErrorsThroughAndDoesNotCacheThem() {
        server.expect(twice(), requestTo(BASE + "/api/schedule/week"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Некорректное название группы\"}"));

        ResponseEntity<byte[]> first = proxy.get("/schedule/week", Map.of());
        ResponseEntity<byte[]> second = proxy.get("/schedule/week", Map.of());

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(text(second)).contains("Некорректное название группы");
        server.verify();
    }

    @Test
    void cachesSuccessfulResponses() {
        server.expect(once(), requestTo(BASE + "/api/teachers"))
                .andRespond(withSuccess("[\"Иванов\"]", MediaType.APPLICATION_JSON));

        proxy.get("/teachers", Map.of());
        ResponseEntity<byte[]> cached = proxy.get("/teachers", Map.of());

        assertThat(text(cached)).isEqualTo("[\"Иванов\"]");
        server.verify();
    }

    @Test
    void unavailableGApiIsBadGateway() {
        server.expect(requestTo(BASE + "/api/rooms")).andRespond(withException(new IOException("refused")));

        ResponseEntity<byte[]> response = proxy.get("/rooms", Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(text(response)).contains("недоступен");
    }

    @Test
    void dropsUnknownParamsSoTheyCannotBustTheCache() {
        server.expect(once(), requestTo(BASE + "/api/schedule?date=2026-09-28"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        proxy.get("/schedule", params("date", "2026-09-28", "junk", "1"));
        proxy.get("/schedule", params("date", "2026-09-28", "junk", "2"));

        server.verify();
    }

    @Test
    void rejectsTooManyParams() {
        Map<String, String[]> many = new LinkedHashMap<>();
        for (int i = 0; i <= GApiProxy.MAX_PARAMS; i++) {
            many.put("p" + i, new String[]{"v"});
        }

        assertThat(proxy.get("/groups", many).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
