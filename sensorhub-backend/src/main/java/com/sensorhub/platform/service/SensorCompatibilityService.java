package com.sensorhub.platform.service;

import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface SensorCompatibilityService {

    ResponseEntity<Map<String, Object>> login(Map<String, Object> payload, HttpServletRequest request);

    ResponseEntity<Map<String, Object>> verifyToken(HttpServletRequest request);

    ResponseEntity<Map<String, Object>> getCurrentUser(HttpServletRequest request);

    ResponseEntity<Map<String, Object>> listUsers(HttpServletRequest request);

    ResponseEntity<Map<String, Object>> createUser(Map<String, Object> payload, HttpServletRequest request);

    ResponseEntity<?> exportData(String dataType,
                                 String format,
                                 String startDate,
                                 String endDate,
                                 String deviceToken);
}
