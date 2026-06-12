package com.sensorhub.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.sensorhub.platform.model.entity.SensorDevice;
import com.sensorhub.platform.model.entity.SensorGpsData;
import com.sensorhub.platform.service.SensorDeviceService;
import com.sensorhub.platform.service.SensorGpsDataService;
import com.sensorhub.platform.service.SensorNorthboundService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SensorNorthboundServiceImpl implements SensorNorthboundService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_SYNC_BATCH_SIZE = 100;
    private static final int MAX_SYNC_BATCH_SIZE = 100;
    private static final int DEFAULT_SYNC_MAX_PAGES = 50;
    private static final int MAX_SYNC_MAX_PAGES = 200;
    private static final long GPS_SECOND_LEVEL_THRESHOLD = 100000000000L;
    private static final long MIN_VALID_GPS_HISTORY_TIMESTAMP_MS = 946684800000L;
    private static final String ESP32_WEATHER_TABLE = "sensor_esp32_weather_data";
    private static final String WEATHER_STATION_1_TABLE = "sensor_weather_station_1_data";
    private static final String WEATHER_STATION_2_TABLE = "sensor_weather_station_2_data";
    private static final String WIND_TABLE = "sensor_wind_data";
    private static final String WIND_OCR_TABLE = "sensor_wind_ocr_data";
    private static final String IMAGE_TABLE = "sensor_image_data";
    private static final String DEFAULT_WEATHER_STATION_1_TOKEN = "weather_station_1_pro_001";
    private static final String DEFAULT_WEATHER_STATION_2_TOKEN = "weather_station_2_pro_002";
    private static final String IMAGE_UPLOAD_RELATIVE_DIR = "uploads/images";
    private static final List<String> JSON_TEXT_COLUMNS = Arrays.asList(
            "detections",
            "metadata",
            "geo_boundary",
            "mars3d_data"
    );

    private final Gson gson = new Gson();

    @Resource
    private SensorDeviceService sensorDeviceService;

    @Resource
    private SensorGpsDataService sensorGpsDataService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private DataSource dataSource;

    @Value("${platform.public-base-url:http://localhost:7529/api}")
    private String platformPublicBaseUrl;

    @Override
    public Map<String, Object> listDevices(Boolean enabled, String type, String keyword) {
        QueryWrapper<SensorDevice> queryWrapper = new QueryWrapper<>();
        if (enabled != null) {
            queryWrapper.eq("status", enabled ? 1 : 0);
        }
        queryWrapper.eq(StringUtils.isNotBlank(type), "deviceType", StringUtils.trim(type));
        if (StringUtils.isNotBlank(keyword)) {
            String safeKeyword = StringUtils.trim(keyword);
            queryWrapper.and(wrapper -> wrapper.like("deviceCode", safeKeyword).or().like("deviceName", safeKeyword));
        }
        queryWrapper.orderByDesc("createTime");

        List<Map<String, Object>> data = sensorDeviceService.list(queryWrapper).stream()
                .map(this::toLegacyDeviceMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        result.put("count", data.size());
        return result;
    }

    @Override
    public Map<String, Object> listGpsData(Integer limit, Integer offset, String deviceToken, String startDate, String endDate) {
        try {
            QueryWrapper<SensorGpsData> queryWrapper = buildGpsQueryWrapper(deviceToken, startDate, endDate);
            queryWrapper.orderByDesc("timestamp");
            queryWrapper.last(buildLimitClause(limit, offset));
            return buildGpsListResponse(sensorGpsDataService.list(queryWrapper));
        } catch (Exception e) {
            log.error("local gps query failed", e);
            return errorResponse("failed to query GPS data", e);
        }
    }

    @Override
    public Map<String, Object> saveGpsData(Map<String, Object> payload) {
        String deviceToken = getStringValue(payload, "device_token", "deviceToken");
        if (StringUtils.isBlank(deviceToken)) {
            return errorResponse("device_token is required");
        }

        try {
            SensorGpsData sensorGpsData = buildGpsDataEntity(payload, deviceToken.trim());
            SensorGpsData existingGpsData = findExistingGpsRecord(sensorGpsData);
            if (existingGpsData != null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("message", "GPS data already exists");
                result.put("deduplicated", true);
                result.put("data", toLegacyGpsMap(existingGpsData));
                return result;
            }
            sensorGpsDataService.save(sensorGpsData);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "GPS data saved successfully");
            result.put("data", toLegacyGpsMap(sensorGpsData));
            return result;
        } catch (Exception e) {
            log.error("save local gps data failed", e);
            return errorResponse("failed to save GPS data", e);
        }
    }

    @Override
    public Map<String, Object> syncGpsHistory(Integer batchSize, Integer maxPages) {
        int invalidLocalRecordsRemoved = cleanupInvalidGpsHistoryRecords();
        long localCountAfter = sensorGpsDataService.count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "GPS history sync endpoint is now local-only; external legacy backfill has been removed");
        result.put("batchSize", normalizeSyncBatchSize(batchSize));
        result.put("maxPages", normalizeSyncMaxPages(maxPages));
        result.put("pagesProcessed", 0);
        result.put("recordsFetchedFromExternal", 0);
        result.put("recordsInserted", 0);
        result.put("recordsSkipped", 0);
        result.put("invalidLocalRecordsRemoved", invalidLocalRecordsRemoved);
        result.put("localGpsCountAfter", localCountAfter);
        result.put("gpsReadMode", "local-only");
        return result;
    }

    @Override
    public Map<String, Object> listEsp32WeatherData(Integer limit, Integer offset, String deviceToken, String startDate, String endDate) {
        return listLocalDataset(ESP32_WEATHER_TABLE, limit, offset, deviceToken, startDate, endDate);
    }

    @Override
    public Map<String, Object> saveEsp32WeatherData(Map<String, Object> payload) {
        String deviceToken = getStringValue(payload, "device_token", "deviceToken");
        if (StringUtils.isBlank(deviceToken)) {
            return errorResponse("device_token is required");
        }
        try {
            LinkedHashMap<String, Object> insertValues = new LinkedHashMap<>();
            insertValues.put("source_record_id", toLong(payload.get("id"), payload.get("source_record_id"), payload.get("sourceRecordId")));
            insertValues.put("device_token", deviceToken.trim());
            insertValues.put("temperature", toDouble(payload.get("temperature")));
            insertValues.put("humidity", toDouble(payload.get("humidity")));
            insertValues.put("windSpeed", toDouble(payload.get("windSpeed"), payload.get("wind_speed")));
            insertValues.put("windScale", toInteger(payload.get("windScale"), payload.get("wind_scale")));
            insertValues.put("windDirectionDegree", toInteger(payload.get("windDirectionDegree"), payload.get("wind_direction_degree")));
            insertValues.put("windDirection", getStringValue(payload, "windDirection", "wind_direction"));
            insertValues.put("rssi", toInteger(payload.get("rssi"), payload.get("RSSI")));
            insertValues.put("freeHeap", toInteger(payload.get("freeHeap"), payload.get("free_heap")));
            insertValues.put("uptime", toLong(payload.get("uptime")));
            putTimestampIfPresent(insertValues, payload);
            Map<String, Object> savedRecord = insertLocalDatasetRecord(ESP32_WEATHER_TABLE, insertValues);
            return buildSavedResponse("ESP32 weather data saved successfully", savedRecord);
        } catch (Exception e) {
            log.error("save local ESP32 weather data failed", e);
            return errorResponse("failed to save ESP32 weather data", e);
        }
    }

    @Override
    public Map<String, Object> listWeatherStation1Data(Integer limit, Integer offset, String deviceToken, String startDate, String endDate) {
        return listLocalDataset(WEATHER_STATION_1_TABLE, limit, offset, deviceToken, startDate, endDate);
    }

    @Override
    public Map<String, Object> saveWeatherStation1Data(Map<String, Object> payload) {
        String deviceToken = StringUtils.defaultIfBlank(
                getStringValue(payload, "device_token", "deviceToken"),
                DEFAULT_WEATHER_STATION_1_TOKEN
        );
        try {
            LinkedHashMap<String, Object> insertValues = new LinkedHashMap<>();
            insertValues.put("source_record_id", toLong(payload.get("id"), payload.get("source_record_id"), payload.get("sourceRecordId")));
            insertValues.put("device_token", deviceToken.trim());
            insertValues.put("RSSI", toInteger(payload.get("RSSI"), payload.get("rssi")));
            insertValues.put("ambientHumidity", toDouble(payload.get("ambientHumidity"), payload.get("ambient_humidity")));
            insertValues.put("ambientTemperature", toDouble(payload.get("ambientTemperature"), payload.get("ambient_temperature")));
            insertValues.put("pow", toDouble(payload.get("pow")));
            insertValues.put("pressure", toDouble(payload.get("pressure")));
            insertValues.put("windDirection", toInteger(payload.get("windDirection"), payload.get("wind_direction")));
            insertValues.put("windScale", toInteger(payload.get("windScale"), payload.get("wind_scale")));
            insertValues.put("windSpeed", toDouble(payload.get("windSpeed"), payload.get("wind_speed")));
            putTimestampIfPresent(insertValues, payload);
            Map<String, Object> savedRecord = insertLocalDatasetRecord(WEATHER_STATION_1_TABLE, insertValues);
            return buildSavedResponse("Weather station 1 data saved successfully", savedRecord);
        } catch (Exception e) {
            log.error("save local weather station 1 data failed", e);
            return errorResponse("failed to save weather station 1 data", e);
        }
    }

    @Override
    public Map<String, Object> listWeatherStation2Data(Integer limit, Integer offset, String deviceToken, String startDate, String endDate) {
        return listLocalDataset(WEATHER_STATION_2_TABLE, limit, offset, deviceToken, startDate, endDate);
    }

    @Override
    public Map<String, Object> saveWeatherStation2Data(Map<String, Object> payload) {
        String deviceToken = StringUtils.defaultIfBlank(
                getStringValue(payload, "device_token", "deviceToken"),
                DEFAULT_WEATHER_STATION_2_TOKEN
        );
        try {
            LinkedHashMap<String, Object> insertValues = new LinkedHashMap<>();
            insertValues.put("source_record_id", toLong(payload.get("id"), payload.get("source_record_id"), payload.get("sourceRecordId")));
            insertValues.put("device_token", deviceToken.trim());
            insertValues.put("RSSI", toInteger(payload.get("RSSI"), payload.get("rssi")));
            insertValues.put("ambientHumidity", toDouble(payload.get("ambientHumidity"), payload.get("ambient_humidity")));
            insertValues.put("ambientTemperature", toDouble(payload.get("ambientTemperature"), payload.get("ambient_temperature")));
            insertValues.put("pow", toDouble(payload.get("pow")));
            insertValues.put("pressure", toDouble(payload.get("pressure")));
            insertValues.put("windDirection", toInteger(payload.get("windDirection"), payload.get("wind_direction")));
            insertValues.put("windScale", toInteger(payload.get("windScale"), payload.get("wind_scale")));
            insertValues.put("windSpeed", toDouble(payload.get("windSpeed"), payload.get("wind_speed")));
            putTimestampIfPresent(insertValues, payload);
            Map<String, Object> savedRecord = insertLocalDatasetRecord(WEATHER_STATION_2_TABLE, insertValues);
            return buildSavedResponse("Weather station 2 data saved successfully", savedRecord);
        } catch (Exception e) {
            log.error("save local weather station 2 data failed", e);
            return errorResponse("failed to save weather station 2 data", e);
        }
    }

    @Override
    public Map<String, Object> listWindData(Integer limit, Integer offset, String deviceToken, String startDate, String endDate) {
        return listLocalDataset(WIND_TABLE, limit, offset, deviceToken, startDate, endDate);
    }

    @Override
    public Map<String, Object> saveWindData(Map<String, Object> payload) {
        String deviceToken = getStringValue(payload, "device_token", "deviceToken");
        if (StringUtils.isBlank(deviceToken)) {
            return errorResponse("device_token is required");
        }
        try {
            LinkedHashMap<String, Object> insertValues = new LinkedHashMap<>();
            insertValues.put("source_record_id", toLong(payload.get("id"), payload.get("source_record_id"), payload.get("sourceRecordId")));
            insertValues.put("device_token", deviceToken.trim());
            insertValues.put("wind_speed", toDouble(payload.get("wind_speed"), payload.get("windSpeed")));
            insertValues.put("windDirectionDegree", toInteger(payload.get("windDirectionDegree"), payload.get("wind_direction_degree")));
            insertValues.put("windDirection", getStringValue(payload, "windDirection", "wind_direction"));
            putTimestampIfPresent(insertValues, payload);
            Map<String, Object> savedRecord = insertLocalDatasetRecord(WIND_TABLE, insertValues);
            return buildSavedResponse("Wind data saved successfully", savedRecord);
        } catch (Exception e) {
            log.error("save local wind data failed", e);
            return errorResponse("failed to save wind data", e);
        }
    }

    @Override
    public Map<String, Object> listWindOcrData(Integer limit, Integer offset, String deviceToken, String startDate, String endDate) {
        return listLocalDataset(WIND_OCR_TABLE, limit, offset, deviceToken, startDate, endDate);
    }

    @Override
    public Map<String, Object> saveWindOcrData(Map<String, Object> payload) {
        String deviceToken = getStringValue(payload, "device_token", "deviceToken");
        if (StringUtils.isBlank(deviceToken)) {
            return errorResponse("device_token is required");
        }
        try {
            LinkedHashMap<String, Object> insertValues = new LinkedHashMap<>();
            insertValues.put("source_record_id", toLong(payload.get("id"), payload.get("source_record_id"), payload.get("sourceRecordId")));
            insertValues.put("device_token", deviceToken.trim());
            insertValues.put("timestamp_millisecond", normalizeTimestampMillisecond(
                    toLong(payload.get("timestamp_millisecond"), payload.get("timestampMillisecond"))
            ));
            insertValues.put("spd", toDouble(payload.get("spd")));
            insertValues.put("wind_speed", toDouble(payload.get("wind_speed"), payload.get("windSpeed")));
            insertValues.put("wind_direction", getStringValue(payload, "wind_direction", "windDirection"));
            putTimestampIfPresent(insertValues, payload);
            Map<String, Object> savedRecord = insertLocalDatasetRecord(WIND_OCR_TABLE, insertValues);
            return buildSavedResponse("Wind OCR data saved successfully", savedRecord);
        } catch (Exception e) {
            log.error("save local wind OCR data failed", e);
            return errorResponse("failed to save wind OCR data", e);
        }
    }

    @Override
    public Map<String, Object> listImageData(Integer limit, Integer offset, String deviceToken, String startDate, String endDate) {
        return listLocalDataset(IMAGE_TABLE, limit, offset, deviceToken, startDate, endDate);
    }

    @Override
    public Map<String, Object> saveImageData(Map<String, Object> payload, MultipartFile imageFile) {
        String deviceToken = getStringValue(payload, "device_token", "deviceToken");
        String imageName = StringUtils.defaultIfBlank(
                getStringValue(payload, "image_name", "imageName"),
                imageFile == null ? null : imageFile.getOriginalFilename()
        );
        if (StringUtils.isBlank(deviceToken) || StringUtils.isBlank(imageName)) {
            return errorResponse("device_token and image_name are required");
        }
        try {
            String imageUrl = getStringValue(payload, "image_url", "imageUrl");
            if (imageFile != null && !imageFile.isEmpty()) {
                imageUrl = storeImageFile(imageFile);
            }
            LinkedHashMap<String, Object> insertValues = new LinkedHashMap<>();
            insertValues.put("source_record_id", toLong(payload.get("id"), payload.get("source_record_id"), payload.get("sourceRecordId")));
            insertValues.put("device_token", deviceToken.trim());
            insertValues.put("image_name", imageName.trim());
            insertValues.put("image_url", imageUrl);
            insertValues.put("width", toInteger(payload.get("width")));
            insertValues.put("height", toInteger(payload.get("height")));
            insertValues.put("file_size", toLong(payload.get("file_size"), payload.get("fileSize")));
            insertValues.put("detections", toJsonText(payload.get("detections")));
            insertValues.put("metadata", toJsonText(payload.get("metadata")));
            insertValues.put("geo_boundary", toJsonText(payload.get("geo_boundary"), payload.get("geoBoundary")));
            insertValues.put("mars3d_data", toJsonText(payload.get("mars3d_data"), payload.get("mars3dData")));
            putTimestampIfPresent(insertValues, payload);
            Map<String, Object> savedRecord = insertLocalDatasetRecord(IMAGE_TABLE, insertValues);
            return buildSavedResponse("Image data saved successfully", savedRecord);
        } catch (Exception e) {
            log.error("save local image data failed", e);
            return errorResponse("failed to save image data", e);
        }
    }

    @Override
    public Map<String, Object> getDataStats(String startDate, String endDate) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("gps", buildLocalGpsStats(startDate, endDate));
        stats.put("ESP32_weather", buildLocalTableStats(ESP32_WEATHER_TABLE, startDate, endDate));
        stats.put("weather_station_1", buildLocalTableStats(WEATHER_STATION_1_TABLE, startDate, endDate));
        stats.put("weather_station_2", buildLocalTableStats(WEATHER_STATION_2_TABLE, startDate, endDate));
        stats.put("wind", buildLocalTableStats(WIND_TABLE, startDate, endDate));
        stats.put("wind_ocr", buildLocalTableStats(WIND_OCR_TABLE, startDate, endDate));
        stats.put("image", buildLocalTableStats(IMAGE_TABLE, startDate, endDate));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", stats);
        result.put("dateRange", buildDateRange(startDate, endDate));
        return result;
    }

    @Override
    public Map<String, Object> getDatabaseDebugInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            Map<String, Object> database = new LinkedHashMap<>();
            database.put("jdbcUrl", metaData == null ? null : metaData.getURL());
            database.put("product", metaData == null ? null : metaData.getDatabaseProductName());
            database.put("productVersion", metaData == null ? null : metaData.getDatabaseProductVersion());
            database.put("schema", connection.getSchema());

            Map<String, Object> mysqlRuntime = new LinkedHashMap<>();
            mysqlRuntime.put("database", querySingleValue("SELECT DATABASE()"));
            mysqlRuntime.put("hostname", querySingleValue("SELECT @@hostname"));
            mysqlRuntime.put("port", querySingleValue("SELECT @@port"));
            mysqlRuntime.put("version", querySingleValue("SELECT @@version"));

            Map<String, Object> counts = new LinkedHashMap<>();
            counts.put("sensor_data_source", countTableRows("sensor_data_source"));
            counts.put("sensor_device", countTableRows("sensor_device"));
            counts.put("sensor_gps_data", countTableRows("sensor_gps_data"));
            counts.put(ESP32_WEATHER_TABLE, countTableRows(ESP32_WEATHER_TABLE));
            counts.put(WEATHER_STATION_1_TABLE, countTableRows(WEATHER_STATION_1_TABLE));
            counts.put(WEATHER_STATION_2_TABLE, countTableRows(WEATHER_STATION_2_TABLE));
            counts.put(WIND_TABLE, countTableRows(WIND_TABLE));
            counts.put(WIND_OCR_TABLE, countTableRows(WIND_OCR_TABLE));
            counts.put(IMAGE_TABLE, countTableRows(IMAGE_TABLE));
            counts.put("sensor_api_endpoint", countTableRows("sensor_api_endpoint"));
            counts.put("sensor_interface_binding", countTableRows("sensor_interface_binding"));
            counts.put("sensor_realtime_channel", countTableRows("sensor_realtime_channel"));

            Map<String, Object> gpsRange = new LinkedHashMap<>();
            gpsRange.put("minTimestampMillisecond", querySingleValue("SELECT MIN(timestampMillisecond) FROM sensor_gps_data"));
            gpsRange.put("maxTimestampMillisecond", querySingleValue("SELECT MAX(timestampMillisecond) FROM sensor_gps_data"));
            gpsRange.put("minTimestamp", querySingleValue("SELECT MIN(`timestamp`) FROM sensor_gps_data"));
            gpsRange.put("maxTimestamp", querySingleValue("SELECT MAX(`timestamp`) FROM sensor_gps_data"));
            gpsRange.put("localReadHasData", hasAnyLocalGpsData());

            result.put("success", true);
            result.put("database", database);
            result.put("mysqlRuntime", mysqlRuntime);
            result.put("tableCounts", counts);
            result.put("gpsRange", gpsRange);
            result.put("gpsReadMode", "local-only");
            result.put("runtimeMode", "mysql-local-only");
            return result;
        } catch (Exception e) {
            log.error("load database debug info failed", e);
            return errorResponse("failed to load database debug info", e);
        }
    }

    private Map<String, Object> listLocalDataset(String tableName,
                                                 Integer limit,
                                                 Integer offset,
                                                 String deviceToken,
                                                 String startDate,
                                                 String endDate) {
        try {
            return buildGenericListResponse(
                    queryLocalDatasetRows(tableName, limit, offset, deviceToken, startDate, endDate)
            );
        } catch (Exception e) {
            log.error("local dataset query failed, table={}", tableName, e);
            return errorResponse("failed to query sensor dataset", e);
        }
    }

    private List<Map<String, Object>> queryLocalDatasetRows(String tableName,
                                                            Integer limit,
                                                            Integer offset,
                                                            String deviceToken,
                                                            String startDate,
                                                            String endDate) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ").append(tableName);
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        if (StringUtils.isNotBlank(deviceToken)) {
            conditions.add("device_token = ?");
            params.add(deviceToken.trim());
        }
        if (StringUtils.isNotBlank(startDate)) {
            conditions.add("`timestamp` >= ?");
            params.add(Timestamp.valueOf(startDate.trim() + " 00:00:00"));
        }
        if (StringUtils.isNotBlank(endDate)) {
            conditions.add("`timestamp` <= ?");
            params.add(Timestamp.valueOf(endDate.trim() + " 23:59:59"));
        }
        if (!conditions.isEmpty()) {
            sqlBuilder.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sqlBuilder.append(" ORDER BY `timestamp` DESC LIMIT ? OFFSET ?");
        params.add(normalizeLimit(limit));
        params.add(normalizeOffset(offset));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
        return rows.stream()
                .map(this::normalizeTelemetryRow)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildGenericListResponse(List<Map<String, Object>> records) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", records);
        result.put("count", records.size());
        return result;
    }

    private Map<String, Object> buildLocalTableStats(String tableName, String startDate, String endDate) {
        try {
            StringBuilder sqlBuilder = new StringBuilder(
                    "SELECT COUNT(1) AS count, MIN(`timestamp`) AS earliest, MAX(`timestamp`) AS latest FROM "
            ).append(tableName);
            List<Object> params = new ArrayList<>();
            List<String> conditions = new ArrayList<>();
            if (StringUtils.isNotBlank(startDate)) {
                conditions.add("`timestamp` >= ?");
                params.add(Timestamp.valueOf(startDate.trim() + " 00:00:00"));
            }
            if (StringUtils.isNotBlank(endDate)) {
                conditions.add("`timestamp` <= ?");
                params.add(Timestamp.valueOf(endDate.trim() + " 23:59:59"));
            }
            if (!conditions.isEmpty()) {
                sqlBuilder.append(" WHERE ").append(String.join(" AND ", conditions));
            }
            Map<String, Object> statsRow = jdbcTemplate.queryForMap(sqlBuilder.toString(), params.toArray());
            long count = toLongValue(statsRow.get("count"));
            if (count <= 0) {
                return emptyStats();
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("count", count);
            result.put("earliest", formatObjectAsDate(statsRow.get("earliest")));
            result.put("latest", formatObjectAsDate(statsRow.get("latest")));
            return result;
        } catch (Exception e) {
            log.warn("build local table stats failed, table={}, reason={}", tableName, e.getMessage());
            return emptyStats();
        }
    }

    private Map<String, Object> insertLocalDatasetRecord(String tableName, LinkedHashMap<String, Object> insertValues) {
        LinkedHashMap<String, Object> filteredValues = new LinkedHashMap<>();
        insertValues.forEach((key, value) -> {
            if (value != null) {
                filteredValues.put(key, value);
            }
        });
        String columnSegment = String.join(", ", filteredValues.keySet());
        String placeholderSegment = filteredValues.keySet().stream()
                .map(key -> "?")
                .collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + tableName + " (" + columnSegment + ") VALUES (" + placeholderSegment + ")";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int index = 1;
            for (Object value : filteredValues.values()) {
                setPreparedStatementValue(preparedStatement, index++, value);
            }
            return preparedStatement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("failed to obtain generated primary key");
        }
        return loadSingleDatasetRecord(tableName, key.longValue());
    }

    private Map<String, Object> loadSingleDatasetRecord(String tableName, Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + tableName + " WHERE id = ? LIMIT 1",
                id
        );
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        return normalizeTelemetryRow(rows.get(0));
    }

    private void setPreparedStatementValue(PreparedStatement preparedStatement, int index, Object value) throws java.sql.SQLException {
        if (value instanceof Timestamp) {
            preparedStatement.setTimestamp(index, (Timestamp) value);
            return;
        }
        if (value instanceof Date) {
            preparedStatement.setTimestamp(index, new Timestamp(((Date) value).getTime()));
            return;
        }
        if (value instanceof Integer) {
            preparedStatement.setInt(index, (Integer) value);
            return;
        }
        if (value instanceof Long) {
            preparedStatement.setLong(index, (Long) value);
            return;
        }
        if (value instanceof Double) {
            preparedStatement.setDouble(index, (Double) value);
            return;
        }
        if (value instanceof Float) {
            preparedStatement.setFloat(index, (Float) value);
            return;
        }
        if (value instanceof byte[]) {
            preparedStatement.setBytes(index, (byte[]) value);
            return;
        }
        preparedStatement.setObject(index, value);
    }

    private Map<String, Object> normalizeTelemetryRow(Map<String, Object> rawRow) {
        Map<String, Object> normalizedRow = new LinkedHashMap<>();
        rawRow.forEach((key, value) -> {
            if ("source_record_id".equalsIgnoreCase(key)) {
                return;
            }
            normalizedRow.put(key, normalizeTelemetryValue(key, value));
        });
        return normalizedRow;
    }

    private Object normalizeTelemetryValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            value = new String((byte[]) value, StandardCharsets.UTF_8);
        }
        if (value instanceof Date) {
            return formatDate((Date) value);
        }
        if (value instanceof LocalDateTime) {
            return value.toString().replace("T", " ");
        }
        if (value instanceof LocalDate || value instanceof LocalTime) {
            return value.toString();
        }
        if (JSON_TEXT_COLUMNS.contains(key)) {
            return parseJsonTextValue(value);
        }
        return value;
    }

    private Object parseJsonTextValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return gson.fromJson(text, Object.class);
        } catch (Exception e) {
            return text;
        }
    }

    private Map<String, Object> buildSavedResponse(String message, Map<String, Object> savedRecord) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.put("data", savedRecord);
        return result;
    }

    private boolean hasAnyLocalRows(String tableName) {
        Long rowCount = countTableRows(tableName);
        return rowCount != null && rowCount > 0;
    }

    private void putTimestampIfPresent(Map<String, Object> insertValues, Map<String, Object> payload) {
        Timestamp timestamp = resolvePayloadTimestamp(payload);
        if (timestamp != null) {
            insertValues.put("timestamp", timestamp);
        }
    }

    private Timestamp resolvePayloadTimestamp(Map<String, Object> payload) {
        Long timestampMillisecond = normalizeTimestampMillisecond(
                toLong(payload.get("timestamp_millisecond"), payload.get("timestampMillisecond"))
        );
        if (timestampMillisecond != null && timestampMillisecond > 0) {
            return new Timestamp(timestampMillisecond);
        }
        String timestampText = getStringValue(payload, "timestamp");
        if (StringUtils.isBlank(timestampText)) {
            return null;
        }
        try {
            return Timestamp.from(Instant.parse(timestampText.trim()));
        } catch (Exception ignored) {
            // fall through
        }
        String normalizedTimestampText = timestampText.trim()
                .replace("T", " ")
                .replace("Z", "");
        try {
            return Timestamp.valueOf(normalizedTimestampText);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String toJsonText(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof String) {
                String text = ((String) value).trim();
                if (StringUtils.isBlank(text)) {
                    continue;
                }
                return text;
            }
            return gson.toJson(value);
        }
        return null;
    }

    private String storeImageFile(MultipartFile imageFile) throws IOException {
        Path uploadDirectory = Paths.get(System.getProperty("user.dir"), IMAGE_UPLOAD_RELATIVE_DIR)
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(uploadDirectory);
        String originalFilename = StringUtils.defaultIfBlank(imageFile.getOriginalFilename(), "image.bin");
        String extension = "";
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex >= 0) {
            extension = originalFilename.substring(extensionIndex);
        }
        String generatedFilename = Instant.now().toEpochMilli()
                + "-"
                + UUID.randomUUID().toString().replace("-", "")
                + extension;
        Path targetPath = uploadDirectory.resolve(generatedFilename);
        imageFile.transferTo(targetPath.toFile());
        return StringUtils.removeEnd(platformPublicBaseUrl, "/") + "/uploads/images/" + generatedFilename;
    }

    private long toLongValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        Long parsedValue = toLong(value);
        return parsedValue == null ? 0L : parsedValue;
    }

    private String formatObjectAsDate(Object value) {
        if (value instanceof Date) {
            return formatDate((Date) value);
        }
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private QueryWrapper<SensorGpsData> buildGpsQueryWrapper(String deviceToken, String startDate, String endDate) {
        QueryWrapper<SensorGpsData> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(deviceToken)) {
            queryWrapper.eq("deviceToken", deviceToken.trim());
        }
        if (StringUtils.isNotBlank(startDate)) {
            queryWrapper.ge("timestamp", startDate.trim() + " 00:00:00");
        }
        if (StringUtils.isNotBlank(endDate)) {
            queryWrapper.le("timestamp", endDate.trim() + " 23:59:59");
        }
        return queryWrapper;
    }

    private String buildLimitClause(Integer limit, Integer offset) {
        int safeLimit = normalizeLimit(limit);
        int safeOffset = normalizeOffset(offset);
        return "LIMIT " + safeLimit + " OFFSET " + safeOffset;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private int normalizeOffset(Integer offset) {
        if (offset == null || offset < 0) {
            return 0;
        }
        return offset;
    }

    private boolean hasScopedDatasetQuery(String deviceToken, String startDate, String endDate, Integer offset) {
        return StringUtils.isNotBlank(deviceToken)
                || StringUtils.isNotBlank(startDate)
                || StringUtils.isNotBlank(endDate)
                || normalizeOffset(offset) > 0;
    }

    private int normalizeSyncBatchSize(Integer batchSize) {
        if (batchSize == null || batchSize <= 0) {
            return DEFAULT_SYNC_BATCH_SIZE;
        }
        return Math.min(batchSize, MAX_SYNC_BATCH_SIZE);
    }

    private int normalizeSyncMaxPages(Integer maxPages) {
        if (maxPages == null || maxPages <= 0) {
            return DEFAULT_SYNC_MAX_PAGES;
        }
        return Math.min(maxPages, MAX_SYNC_MAX_PAGES);
    }

    private SensorGpsData buildGpsDataEntity(Map<String, Object> payload, String deviceToken) {
        SensorGpsData sensorGpsData = new SensorGpsData();
        sensorGpsData.setSourceRecordId(toLong(payload.get("id"), payload.get("sourceRecordId")));
        sensorGpsData.setDeviceToken(deviceToken);
        sensorGpsData.setTimestampMillisecond(
                normalizeTimestampMillisecond(toLong(payload.get("timestamp_millisecond"), payload.get("timestampMillisecond")))
        );
        sensorGpsData.setVelocityX(toDouble(payload.get("velocity_x"), payload.get("velocityX")));
        sensorGpsData.setVelocityY(toDouble(payload.get("velocity_y"), payload.get("velocityY")));
        sensorGpsData.setVelocityZ(toDouble(payload.get("velocity_z"), payload.get("velocityZ")));
        sensorGpsData.setRtkVelocityX(toDouble(payload.get("rtk_velocity_x"), payload.get("rtkVelocityX")));
        sensorGpsData.setRtkVelocityY(toDouble(payload.get("rtk_velocity_y"), payload.get("rtkVelocityY")));
        sensorGpsData.setRtkVelocityZ(toDouble(payload.get("rtk_velocity_z"), payload.get("rtkVelocityZ")));
        sensorGpsData.setGpsVelocityX(toDouble(payload.get("gps_velocity_x"), payload.get("gpsVelocityX")));
        sensorGpsData.setGpsVelocityY(toDouble(payload.get("gps_velocity_y"), payload.get("gpsVelocityY")));
        sensorGpsData.setGpsVelocityZ(toDouble(payload.get("gps_velocity_z"), payload.get("gpsVelocityZ")));
        sensorGpsData.setAccelerationGroundX(toDouble(payload.get("acceleration_ground_x"), payload.get("accelerationGroundX")));
        sensorGpsData.setAccelerationGroundY(toDouble(payload.get("acceleration_ground_y"), payload.get("accelerationGroundY")));
        sensorGpsData.setAccelerationGroundZ(toDouble(payload.get("acceleration_ground_z"), payload.get("accelerationGroundZ")));
        sensorGpsData.setAccelerationBodyX(toDouble(payload.get("acceleration_body_x"), payload.get("accelerationBodyX")));
        sensorGpsData.setAccelerationBodyY(toDouble(payload.get("acceleration_body_y"), payload.get("accelerationBodyY")));
        sensorGpsData.setAccelerationBodyZ(toDouble(payload.get("acceleration_body_z"), payload.get("accelerationBodyZ")));
        sensorGpsData.setAccelerationRawX(toDouble(payload.get("acceleration_raw_x"), payload.get("accelerationRawX")));
        sensorGpsData.setAccelerationRawY(toDouble(payload.get("acceleration_raw_y"), payload.get("accelerationRawY")));
        sensorGpsData.setAccelerationRawZ(toDouble(payload.get("acceleration_raw_z"), payload.get("accelerationRawZ")));
        sensorGpsData.setRtkYaw(toInteger(payload.get("rtk_yaw"), payload.get("rtkYaw")));
        sensorGpsData.setRtkYawInfo(toInteger(payload.get("rtk_yaw_info"), payload.get("rtkYawInfo")));
        sensorGpsData.setGpsDate(toLong(payload.get("gps_date"), payload.get("gpsDate")));
        sensorGpsData.setGpsTime(toLong(payload.get("gps_time"), payload.get("gpsTime")));
        sensorGpsData.setCompassX(toInteger(payload.get("compass_x"), payload.get("compassX")));
        sensorGpsData.setCompassY(toInteger(payload.get("compass_y"), payload.get("compassY")));
        sensorGpsData.setCompassZ(toInteger(payload.get("compass_z"), payload.get("compassZ")));
        sensorGpsData.setQuaternionQ0(toDouble(payload.get("quaternion_q0"), payload.get("quaternionQ0")));
        sensorGpsData.setQuaternionQ1(toDouble(payload.get("quaternion_q1"), payload.get("quaternionQ1")));
        sensorGpsData.setQuaternionQ2(toDouble(payload.get("quaternion_q2"), payload.get("quaternionQ2")));
        sensorGpsData.setQuaternionQ3(toDouble(payload.get("quaternion_q3"), payload.get("quaternionQ3")));
        sensorGpsData.setPitch(toDouble(payload.get("pitch")));
        sensorGpsData.setRoll(toDouble(payload.get("roll")));
        sensorGpsData.setYaw(toDouble(payload.get("yaw")));
        sensorGpsData.setPsi(toDouble(payload.get("psi")));
        sensorGpsData.setGpsPositionX(normalizeLongitude(toDouble(payload.get("gps_position_x"), payload.get("gpsPositionX"))));
        sensorGpsData.setGpsPositionY(normalizeLatitude(toDouble(payload.get("gps_position_y"), payload.get("gpsPositionY"))));
        sensorGpsData.setGpsPositionZ(normalizeAltitude(toDouble(payload.get("gps_position_z"), payload.get("gpsPositionZ"))));

        Long timestampMillisecond = sensorGpsData.getTimestampMillisecond();
        sensorGpsData.setTimestamp(timestampMillisecond != null && timestampMillisecond > 0
                ? new Date(timestampMillisecond)
                : new Date());
        return sensorGpsData;
    }

    private SensorGpsData toGpsEntityForSync(Map<String, Object> payload) {
        String deviceToken = getStringValue(payload, "device_token", "deviceToken");
        if (StringUtils.isBlank(deviceToken)) {
            return null;
        }
        try {
            SensorGpsData sensorGpsData = buildGpsDataEntity(payload, deviceToken.trim());
            if (sensorGpsData.getTimestampMillisecond() == null || sensorGpsData.getTimestampMillisecond() <= 0) {
                return null;
            }
            if (sensorGpsData.getTimestampMillisecond() < MIN_VALID_GPS_HISTORY_TIMESTAMP_MS) {
                return null;
            }
            return sensorGpsData;
        } catch (Exception e) {
            log.warn("skip invalid GPS history record during sync: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> buildLocalGpsStats(String startDate, String endDate) {
        QueryWrapper<SensorGpsData> baseQuery = buildGpsQueryWrapper(null, startDate, endDate);
        long count = sensorGpsDataService.count(baseQuery);
        if (count <= 0) {
            return emptyStats();
        }

        QueryWrapper<SensorGpsData> earliestQuery = buildGpsQueryWrapper(null, startDate, endDate);
        earliestQuery.orderByAsc("timestamp");
        earliestQuery.last("LIMIT 1");

        QueryWrapper<SensorGpsData> latestQuery = buildGpsQueryWrapper(null, startDate, endDate);
        latestQuery.orderByDesc("timestamp");
        latestQuery.last("LIMIT 1");

        SensorGpsData earliest = sensorGpsDataService.getOne(earliestQuery, false);
        SensorGpsData latest = sensorGpsDataService.getOne(latestQuery, false);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("earliest", formatDate(earliest == null ? null : earliest.getTimestamp()));
        result.put("latest", formatDate(latest == null ? null : latest.getTimestamp()));
        return result;
    }

    private Map<String, Object> buildGpsListResponse(List<SensorGpsData> gpsDataList) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", gpsDataList.stream().map(this::toLegacyGpsMap).collect(Collectors.toList()));
        result.put("count", gpsDataList.size());
        return result;
    }

    private boolean hasAnyLocalGpsData() {
        return sensorGpsDataService.count() > 0;
    }

    private Object querySingleValue(String sql) {
        try {
            return jdbcTemplate.queryForObject(sql, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Long countTableRows(String tableName) {
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(1) FROM " + tableName, Long.class);
        } catch (Exception e) {
            log.warn("count table rows failed, table={}, reason={}", tableName, e.getMessage());
            return null;
        }
    }

    private int cleanupInvalidGpsHistoryRecords() {
        QueryWrapper<SensorGpsData> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.isNull("timestampMillisecond")
                .or().le("timestampMillisecond", 0)
                .or().lt("timestampMillisecond", MIN_VALID_GPS_HISTORY_TIMESTAMP_MS));
        long invalidRecordCount = sensorGpsDataService.count(queryWrapper);
        if (invalidRecordCount <= 0) {
            return 0;
        }
        sensorGpsDataService.remove(queryWrapper);
        return (int) invalidRecordCount;
    }

    private Long normalizeTimestampMillisecond(Long timestampMillisecond) {
        if (timestampMillisecond == null || timestampMillisecond <= 0) {
            return timestampMillisecond;
        }
        if (timestampMillisecond < GPS_SECOND_LEVEL_THRESHOLD) {
            return timestampMillisecond * 1000;
        }
        return timestampMillisecond;
    }

    private SensorGpsData findExistingGpsRecord(SensorGpsData sensorGpsData) {
        if (sensorGpsData == null) {
            return null;
        }
        if (sensorGpsData.getSourceRecordId() != null && sensorGpsData.getSourceRecordId() > 0) {
            return sensorGpsDataService.getOne(
                    new QueryWrapper<SensorGpsData>()
                            .eq("sourceRecordId", sensorGpsData.getSourceRecordId())
                            .last("LIMIT 1"),
                    false
            );
        }
        return sensorGpsDataService.getOne(
                new QueryWrapper<SensorGpsData>()
                        .eq("deviceToken", sensorGpsData.getDeviceToken())
                        .eq("timestampMillisecond", sensorGpsData.getTimestampMillisecond())
                        .last("LIMIT 1"),
                false
        );
    }

    private GpsInsertPlan buildGpsInsertPlan(List<SensorGpsData> parsedRecords) {
        Map<String, SensorGpsData> deduplicatedRecordMap = new LinkedHashMap<>();
        int skippedCount = 0;
        for (SensorGpsData parsedRecord : parsedRecords) {
            String uniqueKey = buildGpsUniqueKey(parsedRecord);
            if (uniqueKey == null) {
                skippedCount++;
                continue;
            }
            if (deduplicatedRecordMap.putIfAbsent(uniqueKey, parsedRecord) != null) {
                skippedCount++;
            }
        }
        List<SensorGpsData> deduplicatedRecords = new ArrayList<>(deduplicatedRecordMap.values());
        Set<String> existingKeys = findExistingGpsKeys(deduplicatedRecords);
        List<SensorGpsData> recordsToInsert = deduplicatedRecords.stream()
                .filter(item -> !existingKeys.contains(buildGpsUniqueKey(item)))
                .collect(Collectors.toList());
        skippedCount += deduplicatedRecords.size() - recordsToInsert.size();
        return new GpsInsertPlan(recordsToInsert, skippedCount);
    }

    private Set<String> findExistingGpsKeys(List<SensorGpsData> gpsDataList) {
        if (gpsDataList == null || gpsDataList.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> sourceRecordIdSet = new LinkedHashSet<>();
        Map<String, Set<Long>> timestampMapByDeviceToken = new LinkedHashMap<>();
        for (SensorGpsData sensorGpsData : gpsDataList) {
            if (sensorGpsData.getSourceRecordId() != null && sensorGpsData.getSourceRecordId() > 0) {
                sourceRecordIdSet.add(sensorGpsData.getSourceRecordId());
                continue;
            }
            if (StringUtils.isBlank(sensorGpsData.getDeviceToken()) || sensorGpsData.getTimestampMillisecond() == null) {
                continue;
            }
            timestampMapByDeviceToken
                    .computeIfAbsent(sensorGpsData.getDeviceToken(), key -> new LinkedHashSet<>())
                    .add(sensorGpsData.getTimestampMillisecond());
        }
        Set<String> existingKeys = new LinkedHashSet<>();
        if (!sourceRecordIdSet.isEmpty()) {
            QueryWrapper<SensorGpsData> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("sourceRecordId");
            queryWrapper.in("sourceRecordId", new ArrayList<>(sourceRecordIdSet));
            List<SensorGpsData> existingRecords = sensorGpsDataService.list(queryWrapper);
            for (SensorGpsData existingRecord : existingRecords) {
                String uniqueKey = buildGpsUniqueKey(existingRecord);
                if (uniqueKey != null) {
                    existingKeys.add(uniqueKey);
                }
            }
        }
        for (Map.Entry<String, Set<Long>> entry : timestampMapByDeviceToken.entrySet()) {
            List<Long> timestampList = new ArrayList<>(entry.getValue());
            if (timestampList.isEmpty()) {
                continue;
            }
            QueryWrapper<SensorGpsData> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("deviceToken", "timestampMillisecond");
            queryWrapper.eq("deviceToken", entry.getKey());
            queryWrapper.in("timestampMillisecond", timestampList);
            List<SensorGpsData> existingRecords = sensorGpsDataService.list(queryWrapper);
            for (SensorGpsData existingRecord : existingRecords) {
                String uniqueKey = buildGpsUniqueKey(existingRecord);
                if (uniqueKey != null) {
                    existingKeys.add(uniqueKey);
                }
            }
        }
        return existingKeys;
    }

    private int persistGpsRecords(List<SensorGpsData> gpsDataList) {
        int insertedCount = 0;
        boolean sampleLogged = false;
        for (SensorGpsData sensorGpsData : gpsDataList) {
            if (!sampleLogged) {
                log.info(
                        "gps persist sample sourceRecordId={}, deviceToken={}, timestampMillisecond={}",
                        sensorGpsData.getSourceRecordId(),
                        sensorGpsData.getDeviceToken(),
                        sensorGpsData.getTimestampMillisecond()
                );
                sampleLogged = true;
            }
            try {
                if (sensorGpsDataService.save(sensorGpsData)) {
                    insertedCount++;
                }
            } catch (Exception e) {
                log.warn(
                        "skip GPS history record during persist, deviceToken={}, timestampMillisecond={}, reason={}",
                        sensorGpsData.getDeviceToken(),
                        sensorGpsData.getTimestampMillisecond(),
                        e.getMessage()
                );
            }
        }
        return insertedCount;
    }

    private String buildGpsUniqueKey(SensorGpsData sensorGpsData) {
        if (sensorGpsData != null && sensorGpsData.getSourceRecordId() != null && sensorGpsData.getSourceRecordId() > 0) {
            return "source#" + sensorGpsData.getSourceRecordId();
        }
        if (sensorGpsData == null
                || StringUtils.isBlank(sensorGpsData.getDeviceToken())
                || sensorGpsData.getTimestampMillisecond() == null) {
            return null;
        }
        return sensorGpsData.getDeviceToken().trim() + "#" + sensorGpsData.getTimestampMillisecond();
    }

    private Map<String, Object> toLegacyDeviceMap(SensorDevice sensorDevice) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> configMap = parseJsonObject(sensorDevice.getConfigJson());
        List<Map<String, Object>> dataFields = parseJsonList(sensorDevice.getDataFieldsJson());
        result.put("id", sensorDevice.getDeviceCode());
        result.put("name", sensorDevice.getDeviceName());
        result.put("type", sensorDevice.getDeviceType());
        result.put("category", sensorDevice.getCategory());
        result.put("description", sensorDevice.getDescription());
        result.put("token", sensorDevice.getDeviceToken());
        result.put("data_endpoint", sensorDevice.getDataEndpoint());
        result.put("realtime_key", sensorDevice.getRealtimeKey());
        result.put("enabled", sensorDevice.getStatus() != null && sensorDevice.getStatus() == 1);
        result.put("config", configMap);
        result.put("data_fields", dataFields);
        result.put("icon", configMap.get("icon"));
        result.put("color", configMap.get("color"));
        result.put("created_at", sensorDevice.getCreateTime());
        result.put("updated_at", sensorDevice.getUpdateTime());
        return result;
    }

    private Map<String, Object> toLegacyGpsMap(SensorGpsData sensorGpsData) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", sensorGpsData.getId());
        result.put("source_record_id", sensorGpsData.getSourceRecordId());
        result.put("device_token", sensorGpsData.getDeviceToken());
        result.put("timestamp_millisecond", sensorGpsData.getTimestampMillisecond());
        result.put("velocity_x", sensorGpsData.getVelocityX());
        result.put("velocity_y", sensorGpsData.getVelocityY());
        result.put("velocity_z", sensorGpsData.getVelocityZ());
        result.put("rtk_velocity_x", sensorGpsData.getRtkVelocityX());
        result.put("rtk_velocity_y", sensorGpsData.getRtkVelocityY());
        result.put("rtk_velocity_z", sensorGpsData.getRtkVelocityZ());
        result.put("gps_velocity_x", sensorGpsData.getGpsVelocityX());
        result.put("gps_velocity_y", sensorGpsData.getGpsVelocityY());
        result.put("gps_velocity_z", sensorGpsData.getGpsVelocityZ());
        result.put("acceleration_ground_x", sensorGpsData.getAccelerationGroundX());
        result.put("acceleration_ground_y", sensorGpsData.getAccelerationGroundY());
        result.put("acceleration_ground_z", sensorGpsData.getAccelerationGroundZ());
        result.put("acceleration_body_x", sensorGpsData.getAccelerationBodyX());
        result.put("acceleration_body_y", sensorGpsData.getAccelerationBodyY());
        result.put("acceleration_body_z", sensorGpsData.getAccelerationBodyZ());
        result.put("acceleration_raw_x", sensorGpsData.getAccelerationRawX());
        result.put("acceleration_raw_y", sensorGpsData.getAccelerationRawY());
        result.put("acceleration_raw_z", sensorGpsData.getAccelerationRawZ());
        result.put("rtk_yaw", sensorGpsData.getRtkYaw());
        result.put("rtk_yaw_info", sensorGpsData.getRtkYawInfo());
        result.put("gps_date", sensorGpsData.getGpsDate());
        result.put("gps_time", sensorGpsData.getGpsTime());
        result.put("compass_x", sensorGpsData.getCompassX());
        result.put("compass_y", sensorGpsData.getCompassY());
        result.put("compass_z", sensorGpsData.getCompassZ());
        result.put("quaternion_q0", sensorGpsData.getQuaternionQ0());
        result.put("quaternion_q1", sensorGpsData.getQuaternionQ1());
        result.put("quaternion_q2", sensorGpsData.getQuaternionQ2());
        result.put("quaternion_q3", sensorGpsData.getQuaternionQ3());
        result.put("pitch", sensorGpsData.getPitch());
        result.put("roll", sensorGpsData.getRoll());
        result.put("yaw", sensorGpsData.getYaw());
        result.put("psi", sensorGpsData.getPsi());
        result.put("gps_position_x", sensorGpsData.getGpsPositionX());
        result.put("gps_position_y", sensorGpsData.getGpsPositionY());
        result.put("gps_position_z", sensorGpsData.getGpsPositionZ());
        result.put("timestamp", formatDate(sensorGpsData.getTimestamp()));
        return result;
    }

    private String getStringValue(Map<String, Object> source, String... keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (StringUtils.isBlank(key)) {
                continue;
            }
            Object value = source.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private Double normalizeLongitude(Double value) {
        if (value == null || value.isNaN()) {
            return null;
        }
        return Math.abs(value) > 360 ? value / 10000000D : value;
    }

    private Double normalizeLatitude(Double value) {
        if (value == null || value.isNaN()) {
            return null;
        }
        return Math.abs(value) > 360 ? value / 10000000D : value;
    }

    private Double normalizeAltitude(Double value) {
        if (value == null || value.isNaN()) {
            return null;
        }
        return Math.abs(value) > 10000 ? value / 1000D : value;
    }

    private Double toDouble(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (StringUtils.isBlank(String.valueOf(value))) {
                continue;
            }
            try {
                return Double.parseDouble(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                // try next candidate
            }
        }
        return null;
    }

    private Integer toInteger(Object... values) {
        Double value = toDouble(values);
        return value == null ? null : value.intValue();
    }

    private Long toLong(Object... values) {
        Double value = toDouble(values);
        return value == null ? null : value.longValue();
    }

    private String formatDate(Date date) {
        return date == null ? null : Instant.ofEpochMilli(date.getTime()).toString();
    }

    private Map<String, Object> buildDateRange(String startDate, String endDate) {
        Map<String, Object> dateRange = new LinkedHashMap<>();
        dateRange.put("startDate", StringUtils.isBlank(startDate) ? null : startDate.trim());
        dateRange.put("endDate", StringUtils.isBlank(endDate) ? null : endDate.trim());
        return dateRange;
    }

    private Map<String, Object> emptyStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("count", 0);
        stats.put("earliest", null);
        stats.put("latest", null);
        return stats;
    }

    private Map<String, Object> getNestedMap(Map<String, Object> source, String key, Map<String, Object> defaultValue) {
        if (source == null || source.isEmpty()) {
            return defaultValue;
        }
        Object value = source.get(key);
        Map<String, Object> result = extractTopLevelMap(value);
        return result.isEmpty() ? defaultValue : result;
    }

    private Map<String, Object> extractTopLevelMap(Object value) {
        if (value == null) {
            return Collections.emptyMap();
        }
        JsonElement jsonElement = gson.toJsonTree(value);
        Type mapType = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> result = gson.fromJson(jsonElement, mapType);
        return result == null ? Collections.emptyMap() : result;
    }

    private List<Map<String, Object>> extractTopLevelList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        JsonElement jsonElement = gson.toJsonTree(value);
        Type listType = new TypeToken<List<Map<String, Object>>>() {
        }.getType();
        List<Map<String, Object>> result = gson.fromJson(jsonElement, listType);
        return result == null ? Collections.emptyList() : result;
    }

    private boolean isSuccessResponse(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return false;
        }
        Object success = response.get("success");
        if (success == null) {
            return true;
        }
        if (success instanceof Boolean) {
            return (Boolean) success;
        }
        return Boolean.parseBoolean(String.valueOf(success));
    }

    private Map<String, Object> errorResponse(String message) {
        return errorResponse(message, null);
    }

    private Map<String, Object> errorResponse(String message, Exception e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        if (e != null) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> parseJsonObject(String jsonText) {
        if (StringUtils.isBlank(jsonText)) {
            return new LinkedHashMap<>();
        }
        try {
            Type mapType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> result = gson.fromJson(jsonText, mapType);
            return result == null ? new LinkedHashMap<>() : result;
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private List<Map<String, Object>> parseJsonList(String jsonText) {
        if (StringUtils.isBlank(jsonText)) {
            return Collections.emptyList();
        }
        try {
            Type listType = new TypeToken<List<Map<String, Object>>>() {
            }.getType();
            List<Map<String, Object>> result = gson.fromJson(jsonText, listType);
            return result == null ? Collections.emptyList() : result;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private static class GpsInsertPlan {
        private final List<SensorGpsData> recordsToInsert;
        private final int skippedCount;

        private GpsInsertPlan(List<SensorGpsData> recordsToInsert, int skippedCount) {
            this.recordsToInsert = recordsToInsert;
            this.skippedCount = skippedCount;
        }

        private List<SensorGpsData> getRecordsToInsert() {
            return recordsToInsert;
        }

        private int getSkippedCount() {
            return skippedCount;
        }
    }
}
