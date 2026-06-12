package com.sensorhub.platform.controller;

import com.sensorhub.platform.service.SensorNorthboundService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/sensor")
public class SensorNorthboundController {

    @Resource
    private SensorNorthboundService sensorNorthboundService;

    /**
     * Local northbound device directory endpoint.
     * Response shape stays compatible with the legacy /api/devices contract.
     */
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
        String safeDeviceToken = StringUtils.isNotBlank(deviceToken) ? deviceToken : deviceTokenAlias;
        return sensorNorthboundService.listGpsData(limit, offset, safeDeviceToken, startDate, endDate);
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

    @GetMapping("/esp32-weather-data")
    public Map<String, Object> listEsp32WeatherData(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "device_token", required = false) String deviceToken,
            @RequestParam(value = "deviceToken", required = false) String deviceTokenAlias,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        String safeDeviceToken = StringUtils.isNotBlank(deviceToken) ? deviceToken : deviceTokenAlias;
        return sensorNorthboundService.listEsp32WeatherData(limit, offset, safeDeviceToken, startDate, endDate);
    }

    @PostMapping("/esp32-weather-data")
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
        String safeDeviceToken = StringUtils.isNotBlank(deviceToken) ? deviceToken : deviceTokenAlias;
        return sensorNorthboundService.listWeatherStation1Data(limit, offset, safeDeviceToken, startDate, endDate);
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
        String safeDeviceToken = StringUtils.isNotBlank(deviceToken) ? deviceToken : deviceTokenAlias;
        return sensorNorthboundService.listWeatherStation2Data(limit, offset, safeDeviceToken, startDate, endDate);
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
        String safeDeviceToken = StringUtils.isNotBlank(deviceToken) ? deviceToken : deviceTokenAlias;
        return sensorNorthboundService.listWindData(limit, offset, safeDeviceToken, startDate, endDate);
    }

    @PostMapping("/wind-data")
    public Map<String, Object> saveWindData(@RequestBody Map<String, Object> payload) {
        return sensorNorthboundService.saveWindData(payload);
    }

    @GetMapping("/wind-ocr-data")
    public Map<String, Object> listWindOcrData(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "device_token", required = false) String deviceToken,
            @RequestParam(value = "deviceToken", required = false) String deviceTokenAlias,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        String safeDeviceToken = StringUtils.isNotBlank(deviceToken) ? deviceToken : deviceTokenAlias;
        return sensorNorthboundService.listWindOcrData(limit, offset, safeDeviceToken, startDate, endDate);
    }

    @PostMapping("/wind-ocr-data")
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
        String safeDeviceToken = StringUtils.isNotBlank(deviceToken) ? deviceToken : deviceTokenAlias;
        return sensorNorthboundService.listImageData(limit, offset, safeDeviceToken, startDate, endDate);
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

    @GetMapping("/debug/db")
    public Map<String, Object> getDatabaseDebugInfo() {
        return sensorNorthboundService.getDatabaseDebugInfo();
    }
}
