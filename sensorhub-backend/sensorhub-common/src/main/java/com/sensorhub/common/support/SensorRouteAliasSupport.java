package com.sensorhub.common.support;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared route aliases used while sensor devices are being migrated
 * from legacy URLs to canonical SensorHub routes.
 */
public final class SensorRouteAliasSupport {

    private static final Map<String, String> LEGACY_TO_CANONICAL_API_PATH_MAP;

    static {
        Map<String, String> aliasMap = new LinkedHashMap<>();
        aliasMap.put("/api/devices", "/api/sensor/devices");
        aliasMap.put("/api/gps-data", "/api/sensor/gps-data");
        aliasMap.put("/api/ESP32-weather-data", "/api/sensor/esp32-weather-data");
        aliasMap.put("/api/weather-station-1-data", "/api/sensor/weather-station-1-data");
        aliasMap.put("/api/weather-station-2-data", "/api/sensor/weather-station-2-data");
        aliasMap.put("/api/wind-data", "/api/sensor/wind-data");
        aliasMap.put("/api/WindfromM300", "/api/sensor/wind-ocr-data");
        aliasMap.put("/api/image-data", "/api/sensor/image-data");
        aliasMap.put("/api/data-stats", "/api/sensor/data-stats");
        aliasMap.put("/api/auth/user", "/api/sensor/auth/user");
        aliasMap.put("/api/auth/verify", "/api/sensor/auth/verify");
        aliasMap.put("/api/auth/login", "/api/sensor/auth/login");
        aliasMap.put("/api/users", "/api/sensor/users");
        aliasMap.put("/api/export-data", "/api/sensor/export-data");
        LEGACY_TO_CANONICAL_API_PATH_MAP = Collections.unmodifiableMap(aliasMap);
    }

    private SensorRouteAliasSupport() {
    }

    public static Map<String, String> getLegacyToCanonicalApiPathMap() {
        return LEGACY_TO_CANONICAL_API_PATH_MAP;
    }

    public static String canonicalizeApiPath(String rawPath) {
        if (StringUtils.isBlank(rawPath)) {
            return rawPath;
        }
        String path = extractPath(rawPath.trim());
        if (StringUtils.isBlank(path)) {
            return rawPath;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String mappedPath = LEGACY_TO_CANONICAL_API_PATH_MAP.get(path);
        if (StringUtils.isNotBlank(mappedPath)) {
            return mappedPath;
        }

        if (path.startsWith("/api/name/") || path.startsWith("/api/sensor/")) {
            return path;
        }
        if (path.startsWith("/name/")) {
            return "/api" + path;
        }
        if (path.startsWith("/sensor/")) {
            return "/api" + path;
        }
        if (path.startsWith("/api/")) {
            return path;
        }
        return "/api" + path;
    }

    public static String extractPath(String rawPath) {
        if (StringUtils.isBlank(rawPath)) {
            return rawPath;
        }
        String text = rawPath.trim();
        if (text.startsWith("http://") || text.startsWith("https://")) {
            try {
                String uriPath = URI.create(text).getPath();
                if (StringUtils.isNotBlank(uriPath)) {
                    return uriPath;
                }
            } catch (Exception ignored) {
            }
        }
        return text;
    }
}
