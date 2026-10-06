package com.pavlent1yy.gcore.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Component
public class GApiProxy {

    static final int MAX_PARAMS = 10;
    static final Set<String> ALLOWED_PARAMS = Set.of("group", "date");
    private static final byte[] UNAVAILABLE = "{\"error\":\"Сервис расписания недоступен\"}"
            .getBytes(StandardCharsets.UTF_8);

    private final RestClient gApiRestClient;
    private final Cache<String, ResponseEntity<byte[]>> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofSeconds(60))
            .build();

    public GApiProxy(RestClient gApiRestClient) {
        this.gApiRestClient = gApiRestClient;
    }

    public ResponseEntity<byte[]> get(String path, Map<String, String[]> parameters) {
        if (parameters.size() > MAX_PARAMS) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Слишком много параметров\"}".getBytes(StandardCharsets.UTF_8));
        }

        Map<String, String[]> params = new TreeMap<>(parameters);
        params.keySet().retainAll(ALLOWED_PARAMS);

        String key = cacheKey(path, params);
        ResponseEntity<byte[]> cached = cache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }

        ResponseEntity<byte[]> response;
        try {
            response = fetch(path, params);
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(UNAVAILABLE);
        }

        if (response.getStatusCode().is2xxSuccessful()) {
            cache.put(key, response);
        }
        return response;
    }

    private ResponseEntity<byte[]> fetch(String path, Map<String, String[]> params) {
        return gApiRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api" + path);
                    Map<String, Object> values = new HashMap<>();
                    int index = 0;
                    for (Map.Entry<String, String[]> param : params.entrySet()) {
                        for (String value : param.getValue()) {
                            String name = "p" + index++;
                            uriBuilder.queryParam("{" + name + "n}", "{" + name + "}");
                            values.put(name + "n", param.getKey());
                            values.put(name, value);
                        }
                    }
                    return uriBuilder.build(values);
                })
                .exchange((request, response) -> {
                    ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.getStatusCode());
                    MediaType contentType = response.getHeaders().getContentType();
                    if (contentType != null) {
                        builder.contentType(contentType);
                    }
                    return builder.body(response.getBody().readAllBytes());
                });
    }

    private static String cacheKey(String path, Map<String, String[]> params) {
        StringBuilder key = new StringBuilder(path);
        params.forEach((name, values) -> {
            for (String value : values) {
                key.append('\u0000').append(name).append('=').append(value);
            }
        });
        return key.toString();
    }
}
