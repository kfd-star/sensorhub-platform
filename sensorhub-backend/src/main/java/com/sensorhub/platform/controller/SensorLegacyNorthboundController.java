package com.sensorhub.platform.controller;

import com.sensorhub.platform.service.SensorCompatibilityService;
import com.sensorhub.platform.service.SensorNorthboundService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps legacy direct sensor URLs available while devices are migrated
 * to the canonical /api/sensor/* routes.
 */
@RestController
public class SensorLegacyNorthboundController {

    @Resource
    private SensorNorthboundService sensorNorthboundService;

    @Resource
    private SensorCompatibilityService sensorCompatibilityService;

    @GetMapping("/devices")
    public Map<String, Object> listDevices(
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return sensorNorthboundService.listDevices(enabled, type, keyword);
    }

    @GetMapping("/gps-data")
    public Map<String, Object> listGpsData(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "device_token", required = false) String deviceToken,
            @RequestParam(value = "deviceToken", required = false) String deviceTokenAlias,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return sensorNorthboundService.listGpsData(limit, offset, resolveDeviceToken(deviceToken, deviceTokenAlias), startDate, endDate);
    }

    @PostMapping("/gps-data")
    public Map<String, Object> saveGpsData(@RequestBody Map<String, Object> payload) {
        return sensorNorthboundService.saveGpsData(payload);
    }

    @PostMapping("/gps-data/sync")
    public Map<String, Object> syncGpsHistory(
            @RequestParam(value = "batchSize", required = false) Integer batchSize,
            @RequestParam(value = "maxPages", required = false) Integer maxPages
    ) {
        return sensorNorthboundService.syncGpsHistory(batchSize, maxPages);
    }

    @GetMapping("/ESP32-weather-data")
    public Map<String, Object> listEsp32WeatherData(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "device_token", required = false) String deviceToken,
            @RequestParam(value = "deviceToken", required = false) String deviceTokenAlias,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return sensorNorthboundService.listEsp32WeatherData(limit, offset, resolveDeviceToken(deviceToken, deviceTokenAlias), startDate, endDate);
    }

    @PostMapping("/ESP32-weather-data")
    public Map<String, Object> saveEsp32WeatherData(@RequestBody Map<String, Object> payload) {
        return sensorNorthboundService.saveEsp32WeatherData(payload);
    }

    @GetMapping("/weather-station-1-data")
    public Map<String, Object> listWeatherStation1Data(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "device_token", required = false) String deviceToken,
            @RequestParam(value = "deviceToken", required = false) String deviceTokenAlias,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return sensorNorthboundService.listWeatherStation1Data(limit, offset, resolveDeviceToken(deviceToken, deviceTokenAlias), startDate, endDate);
    }

    @PostMapping("/weather-station-1-data")
    public Map<String, Object> saveWeatherStation1Data(@RequestBody Map<String, Object> payload) {
        return sensorNorthboundService.saveWeatherStation1Data(payload);
    }

    @GetMapping("/weather-station-2-data")
    public Map<String, Object> listWeatherStation2Data(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "device_token", required = false) String deviceToken,
            @RequestParam(value = "deviceToken", required = false) String deviceTokenAlias,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return sensorNorthboundService.listWeatherStation2Data(limit, offset, resolveDeviceToken(deviceToken, deviceTokenAlias), startDate, endDate);
    }

    @PostMapping("/weather-station-2-data")
    public Map<String, Object> saveWeatherStation2Data(@RequestBody Map<String, Object> payload) {
        return sensorNorthboundService.saveWeatherStation2Data(payload);
    }

    @GetMapping("/wind-data")
    public Map<String, Object> listWindData(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "device_token", required = false) String deviceToken,
            @RequestParam(value = "deviceToken", required = false) String deviceTokenAlias,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return sensorNorthboundService.listWindData(limit, offset, resolveDeviceToken(deviceToken, deviceTokenAlias), startDate, endDate);
    }

    @PostMapping("/wind-data")
    public Map<String, Object> saveWindData(@RequestBody Map<String, Object> payload) {
        return sensorNorthboundService.saveWindData(payload);
    }

    @GetMapping("/WindfromM300")
    public Map<String, Object> listWindOcrData(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "device_token", required = false) String deviceToken,
            @RequestParam(value = "deviceToken", required = false) String deviceTokenAlias,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return sensorNorthboundService.listWindOcrData(limit, offset, resolveDeviceToken(deviceToken, deviceTokenAlias), startDate, endDate);
    }

    @PostMapping("/WindfromM300")
    public Map<String, Object> saveWindOcrData(@RequestBody Map<String, Object> payload) {
        return sensorNorthboundService.saveWindOcrData(payload);
    }

    @GetMapping("/image-data")
    public Map<String, Object> listImageData(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "device_token", required = false) String deviceToken,
            @RequestParam(value = "deviceToken", required = false) String deviceTokenAlias,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return sensorNorthboundService.listImageData(limit, offset, resolveDeviceToken(deviceToken, deviceTokenAlias), startDate, endDate);
    }

    @PostMapping(value = "/image-data", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> saveImageDataByJson(@RequestBody Map<String, Object> payload) {
        return sensorNorthboundService.saveImageData(payload, null);
    }

    @PostMapping(value = "/image-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> saveImageDataByMultipart(
            @RequestParam Map<String, String> payload,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.putAll(payload);
        return sensorNorthboundService.saveImageData(requestBody, image);
    }

    @GetMapping("/data-stats")
    public Map<String, Object> getDataStats(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return sensorNorthboundService.getDataStats(startDate, endDate);
    }

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
        return sensorCompatibilityService.exportData(
                dataType,
                format,
                startDate,
                endDate,
                resolveDeviceToken(deviceToken, deviceTokenAlias)
        );
    }

    private String resolveDeviceToken(String deviceToken, String deviceTokenAlias) {
        return StringUtils.isNotBlank(deviceToken) ? deviceToken : deviceTokenAlias;
    }
}
