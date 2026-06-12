package com.sensorhub.platform.controller;

import com.sensorhub.platform.service.SensorCompatibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/sensor")
public class SensorCompatibilityController {

    @Resource
    private SensorCompatibilityService sensorCompatibilityService;

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody(required = false) Map<String, Object> payload,
                                                     HttpServletRequest request) {
        return sensorCompatibilityService.login(payload, request);
    }

    @GetMapping("/auth/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(HttpServletRequest request) {
        return sensorCompatibilityService.verifyToken(request);
    }

    @GetMapping("/auth/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        return sensorCompatibilityService.getCurrentUser(request);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers(HttpServletRequest request) {
        return sensorCompatibilityService.listUsers(request);
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody(required = false) Map<String, Object> payload,
                                                          HttpServletRequest request) {
        return sensorCompatibilityService.createUser(payload, request);
    }

    @GetMapping("/export-data")
    public ResponseEntity<?> exportData(@RequestParam(value = "dataType", required = false) String dataType,
                                        @RequestParam(value = "format", required = false) String format,
                                        @RequestParam(value = "startDate", required = false) String startDate,
                                        @RequestParam(value = "endDate", required = false) String endDate,
                                        @RequestParam(value = "device_token", required = false) String deviceToken,
                                        @RequestParam(value = "deviceToken", required = false) String deviceTokenAlias) {
        String safeDeviceToken = deviceToken != null && !deviceToken.trim().isEmpty() ? deviceToken : deviceTokenAlias;
        return sensorCompatibilityService.exportData(dataType, format, startDate, endDate, safeDeviceToken);
    }
}
