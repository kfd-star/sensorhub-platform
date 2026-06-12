package com.sensorhub.platform.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface SensorNorthboundService {

    /**
     * Local replacement for the legacy /api/devices endpoint.
     */
    Map<String, Object> listDevices(Boolean enabled, String type, String keyword);

    /**
     * Local-first replacement for the legacy /api/gps-data endpoint.
     */
    Map<String, Object> listGpsData(Integer limit, Integer offset, String deviceToken, String startDate, String endDate);

    /**
     * Local GPS write endpoint.
     */
    Map<String, Object> saveGpsData(Map<String, Object> payload);

    /**
     * Local GPS history sync endpoint kept for compatibility.
     */
    Map<String, Object> syncGpsHistory(Integer batchSize, Integer maxPages);

    Map<String, Object> listEsp32WeatherData(Integer limit, Integer offset, String deviceToken, String startDate, String endDate);

    Map<String, Object> saveEsp32WeatherData(Map<String, Object> payload);

    Map<String, Object> listWeatherStation1Data(Integer limit, Integer offset, String deviceToken, String startDate, String endDate);

    Map<String, Object> saveWeatherStation1Data(Map<String, Object> payload);

    Map<String, Object> listWeatherStation2Data(Integer limit, Integer offset, String deviceToken, String startDate, String endDate);

    Map<String, Object> saveWeatherStation2Data(Map<String, Object> payload);

    Map<String, Object> listWindData(Integer limit, Integer offset, String deviceToken, String startDate, String endDate);

    Map<String, Object> saveWindData(Map<String, Object> payload);

    Map<String, Object> listWindOcrData(Integer limit, Integer offset, String deviceToken, String startDate, String endDate);

    Map<String, Object> saveWindOcrData(Map<String, Object> payload);

    Map<String, Object> listImageData(Integer limit, Integer offset, String deviceToken, String startDate, String endDate);

    Map<String, Object> saveImageData(Map<String, Object> payload, MultipartFile imageFile);

    /**
     * Local-first stats endpoint.
     */
    Map<String, Object> getDataStats(String startDate, String endDate);

    /**
     * Runtime database self-check endpoint.
     */
    Map<String, Object> getDatabaseDebugInfo();
}
