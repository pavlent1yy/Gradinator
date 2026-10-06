package com.pavlent1yy.gcore.controller;

import com.pavlent1yy.gcore.client.GApiProxy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GApiProxyController {

    public static final String[] PUBLIC_PATHS = {
            "/schedule",
            "/schedule/today",
            "/schedule/tomorrow",
            "/schedule/yesterday",
            "/schedule/week",
            "/schedule/current-weektype",
            "/groups",
            "/groups/departments",
            "/groups/find-department",
            "/groups/department-names",
            "/teachers",
            "/subjects",
            "/rooms"
    };

    private final GApiProxy gApiProxy;

    @GetMapping({
            "/schedule",
            "/schedule/today",
            "/schedule/tomorrow",
            "/schedule/yesterday",
            "/schedule/week",
            "/schedule/current-weektype",
            "/groups",
            "/groups/departments",
            "/groups/find-department",
            "/groups/department-names",
            "/teachers",
            "/subjects",
            "/rooms"
    })
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) {
        return gApiProxy.get(request.getRequestURI(), request.getParameterMap());
    }
}
