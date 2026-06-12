package com.sensorhub.platform.config;

import com.sensorhub.common.support.SensorRouteAliasSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Initialize sensor registry tables and apply schema/data migrations in MySQL.
 */
@Component
@Slf4j
public class SensorRegistrySchemaInitializer implements CommandLineRunner {

    private static final String DEFAULT_SOURCE_CODE = "API_EXAMPLE_LOCAL";
    private static final String DEPRECATED_SOURCE_CODE = "SDS_NODE_LOCAL";
    private static final String DEFAULT_SOURCE_NAME = "SensorHub Local Data Source";
    private static final String CURRENT_ENDPOINT_CODE_PREFIX = "SENSOR_";
    private static final String DEPRECATED_ENDPOINT_CODE_PREFIX = "SDS_";
    private static final String CURRENT_CHANNEL_CODE_PREFIX = "SENSOR_WS_";
    private static final String DEPRECATED_CHANNEL_CODE_PREFIX = "SDS_WS_";

    private static final String DEFAULT_TARGET_SERVICE = "sensorhub-backend";

    private static final String[] LOCAL_PLATFORM_ENDPOINT_PATHS = new String[]{
            "/api/sensor/auth/user",
            "/api/sensor/auth/verify",
            "/api/sensor/auth/login",
            "/api/sensor/users",
            "/api/sensor/export-data",
            "/api/sensor/devices",
            "/api/sensor/gps-data",
            "/api/sensor/esp32-weather-data",
            "/api/sensor/weather-station-1-data",
            "/api/sensor/weather-station-2-data",
            "/api/sensor/wind-data",
            "/api/sensor/wind-ocr-data",
            "/api/sensor/image-data",
            "/api/sensor/data-stats"
    };

    private static final String[] DDL_LIST = new String[]{
            "CREATE TABLE IF NOT EXISTS sensor_data_source ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "code VARCHAR(64) NOT NULL,"
                    + "name VARCHAR(128) NOT NULL,"
                    + "`type` VARCHAR(32) NOT NULL,"
                    + "baseUrl VARCHAR(512) DEFAULT NULL,"
                    + "wsUrl VARCHAR(512) DEFAULT NULL,"
                    + "status TINYINT NOT NULL DEFAULT 1,"
                    + "configJson TEXT DEFAULT NULL,"
                    + "createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "isDelete TINYINT NOT NULL DEFAULT 0,"
                    + "UNIQUE KEY uk_sensor_data_source_code (code)"
                    + ") COMMENT='sensor data source'",
            "CREATE TABLE IF NOT EXISTS sensor_device ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "deviceCode VARCHAR(128) NOT NULL,"
                    + "deviceName VARCHAR(256) NOT NULL,"
                    + "deviceType VARCHAR(64) NOT NULL,"
                    + "category VARCHAR(64) DEFAULT NULL,"
                    + "description VARCHAR(512) DEFAULT NULL,"
                    + "deviceToken VARCHAR(256) DEFAULT NULL,"
                    + "dataSourceId BIGINT DEFAULT NULL,"
                    + "legacyDeviceId VARCHAR(128) DEFAULT NULL,"
                    + "dataEndpoint VARCHAR(512) DEFAULT NULL,"
                    + "realtimeKey VARCHAR(256) DEFAULT NULL,"
                    + "configJson TEXT DEFAULT NULL,"
                    + "dataFieldsJson TEXT DEFAULT NULL,"
                    + "status TINYINT NOT NULL DEFAULT 1,"
                    + "createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "isDelete TINYINT NOT NULL DEFAULT 0,"
                    + "UNIQUE KEY uk_sensor_device_code (deviceCode),"
                    + "KEY idx_sensor_device_type (deviceType),"
                    + "KEY idx_sensor_device_source (dataSourceId),"
                    + "KEY idx_sensor_device_legacy_id (legacyDeviceId)"
                    + ") COMMENT='sensor device'",
            "CREATE TABLE IF NOT EXISTS sensor_gps_data ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "sourceRecordId BIGINT DEFAULT NULL,"
                    + "deviceToken VARCHAR(100) NOT NULL,"
                    + "timestampMillisecond BIGINT DEFAULT NULL,"
                    + "velocityX DOUBLE DEFAULT NULL,"
                    + "velocityY DOUBLE DEFAULT NULL,"
                    + "velocityZ DOUBLE DEFAULT NULL,"
                    + "rtkVelocityX DOUBLE DEFAULT NULL,"
                    + "rtkVelocityY DOUBLE DEFAULT NULL,"
                    + "rtkVelocityZ DOUBLE DEFAULT NULL,"
                    + "gpsVelocityX DOUBLE DEFAULT NULL,"
                    + "gpsVelocityY DOUBLE DEFAULT NULL,"
                    + "gpsVelocityZ DOUBLE DEFAULT NULL,"
                    + "accelerationGroundX DOUBLE DEFAULT NULL,"
                    + "accelerationGroundY DOUBLE DEFAULT NULL,"
                    + "accelerationGroundZ DOUBLE DEFAULT NULL,"
                    + "accelerationBodyX DOUBLE DEFAULT NULL,"
                    + "accelerationBodyY DOUBLE DEFAULT NULL,"
                    + "accelerationBodyZ DOUBLE DEFAULT NULL,"
                    + "accelerationRawX DOUBLE DEFAULT NULL,"
                    + "accelerationRawY DOUBLE DEFAULT NULL,"
                    + "accelerationRawZ DOUBLE DEFAULT NULL,"
                    + "rtkYaw INT DEFAULT NULL,"
                    + "rtkYawInfo INT DEFAULT NULL,"
                    + "gpsDate BIGINT DEFAULT NULL,"
                    + "gpsTime BIGINT DEFAULT NULL,"
                    + "compassX INT DEFAULT NULL,"
                    + "compassY INT DEFAULT NULL,"
                    + "compassZ INT DEFAULT NULL,"
                    + "quaternionQ0 DOUBLE DEFAULT NULL,"
                    + "quaternionQ1 DOUBLE DEFAULT NULL,"
                    + "quaternionQ2 DOUBLE DEFAULT NULL,"
                    + "quaternionQ3 DOUBLE DEFAULT NULL,"
                    + "pitch DOUBLE DEFAULT NULL,"
                    + "roll DOUBLE DEFAULT NULL,"
                    + "yaw DOUBLE DEFAULT NULL,"
                    + "psi DOUBLE DEFAULT NULL,"
                    + "gpsPositionX DOUBLE DEFAULT NULL,"
                    + "gpsPositionY DOUBLE DEFAULT NULL,"
                    + "gpsPositionZ DOUBLE DEFAULT NULL,"
                    + "`timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE KEY uk_sensor_gps_data_source_record (sourceRecordId),"
                    + "KEY idx_sensor_gps_data_device_token (deviceToken),"
                    + "KEY idx_sensor_gps_data_timestamp (`timestamp`),"
                    + "KEY idx_sensor_gps_data_timestamp_ms (timestampMillisecond)"
                    + ") COMMENT='sensor gps telemetry data'",
            "CREATE TABLE IF NOT EXISTS sensor_esp32_weather_data ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "source_record_id BIGINT DEFAULT NULL,"
                    + "device_token VARCHAR(100) NOT NULL,"
                    + "temperature DECIMAL(10,2) DEFAULT NULL,"
                    + "humidity DECIMAL(10,2) DEFAULT NULL,"
                    + "windSpeed DECIMAL(10,2) DEFAULT NULL,"
                    + "windScale INT DEFAULT NULL,"
                    + "windDirectionDegree INT DEFAULT NULL,"
                    + "windDirection VARCHAR(64) DEFAULT NULL,"
                    + "rssi INT DEFAULT NULL,"
                    + "freeHeap INT DEFAULT NULL,"
                    + "uptime BIGINT DEFAULT NULL,"
                    + "`timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE KEY uk_sensor_esp32_weather_source_record (source_record_id),"
                    + "KEY idx_sensor_esp32_weather_device_token (device_token),"
                    + "KEY idx_sensor_esp32_weather_timestamp (`timestamp`)"
                    + ") COMMENT='sensor esp32 weather data'",
            "CREATE TABLE IF NOT EXISTS sensor_weather_station_1_data ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "source_record_id BIGINT DEFAULT NULL,"
                    + "device_token VARCHAR(100) NOT NULL DEFAULT 'weather_station_1_pro_001',"
                    + "`RSSI` INT DEFAULT NULL,"
                    + "ambientHumidity DECIMAL(10,2) DEFAULT NULL,"
                    + "ambientTemperature DECIMAL(10,2) DEFAULT NULL,"
                    + "pow DECIMAL(10,2) DEFAULT NULL,"
                    + "pressure DECIMAL(12,2) DEFAULT NULL,"
                    + "windDirection INT DEFAULT NULL,"
                    + "windScale INT DEFAULT NULL,"
                    + "windSpeed DECIMAL(10,2) DEFAULT NULL,"
                    + "`timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE KEY uk_sensor_weather_station_1_source_record (source_record_id),"
                    + "KEY idx_sensor_weather_station_1_device_token (device_token),"
                    + "KEY idx_sensor_weather_station_1_timestamp (`timestamp`)"
                    + ") COMMENT='sensor weather station 1 data'",
            "CREATE TABLE IF NOT EXISTS sensor_weather_station_2_data ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "source_record_id BIGINT DEFAULT NULL,"
                    + "device_token VARCHAR(100) NOT NULL DEFAULT 'weather_station_2_pro_002',"
                    + "`RSSI` INT DEFAULT NULL,"
                    + "ambientHumidity DECIMAL(10,2) DEFAULT NULL,"
                    + "ambientTemperature DECIMAL(10,2) DEFAULT NULL,"
                    + "pow DECIMAL(10,2) DEFAULT NULL,"
                    + "pressure DECIMAL(12,2) DEFAULT NULL,"
                    + "windDirection INT DEFAULT NULL,"
                    + "windScale INT DEFAULT NULL,"
                    + "windSpeed DECIMAL(10,2) DEFAULT NULL,"
                    + "`timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE KEY uk_sensor_weather_station_2_source_record (source_record_id),"
                    + "KEY idx_sensor_weather_station_2_device_token (device_token),"
                    + "KEY idx_sensor_weather_station_2_timestamp (`timestamp`)"
                    + ") COMMENT='sensor weather station 2 data'",
            "CREATE TABLE IF NOT EXISTS sensor_wind_data ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "source_record_id BIGINT DEFAULT NULL,"
                    + "device_token VARCHAR(100) NOT NULL,"
                    + "wind_speed DECIMAL(10,2) DEFAULT NULL,"
                    + "windDirectionDegree INT DEFAULT NULL,"
                    + "windDirection VARCHAR(64) DEFAULT NULL,"
                    + "`timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE KEY uk_sensor_wind_source_record (source_record_id),"
                    + "KEY idx_sensor_wind_device_token (device_token),"
                    + "KEY idx_sensor_wind_timestamp (`timestamp`)"
                    + ") COMMENT='sensor wind data'",
            "CREATE TABLE IF NOT EXISTS sensor_wind_ocr_data ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "source_record_id BIGINT DEFAULT NULL,"
                    + "device_token VARCHAR(100) NOT NULL,"
                    + "timestamp_millisecond BIGINT DEFAULT NULL,"
                    + "spd DECIMAL(12,3) DEFAULT NULL,"
                    + "wind_speed DECIMAL(12,3) DEFAULT NULL,"
                    + "wind_direction VARCHAR(64) DEFAULT NULL,"
                    + "`timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE KEY uk_sensor_wind_ocr_source_record (source_record_id),"
                    + "KEY idx_sensor_wind_ocr_device_token (device_token),"
                    + "KEY idx_sensor_wind_ocr_timestamp_ms (timestamp_millisecond),"
                    + "KEY idx_sensor_wind_ocr_timestamp (`timestamp`)"
                    + ") COMMENT='sensor wind ocr data'",
            "CREATE TABLE IF NOT EXISTS sensor_image_data ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "source_record_id BIGINT DEFAULT NULL,"
                    + "device_token VARCHAR(100) NOT NULL,"
                    + "image_name VARCHAR(255) NOT NULL,"
                    + "image_url TEXT DEFAULT NULL,"
                    + "width INT DEFAULT NULL,"
                    + "height INT DEFAULT NULL,"
                    + "file_size BIGINT DEFAULT NULL,"
                    + "detections LONGTEXT DEFAULT NULL,"
                    + "metadata LONGTEXT DEFAULT NULL,"
                    + "geo_boundary LONGTEXT DEFAULT NULL,"
                    + "mars3d_data LONGTEXT DEFAULT NULL,"
                    + "`timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE KEY uk_sensor_image_source_record (source_record_id),"
                    + "KEY idx_sensor_image_device_token (device_token),"
                    + "KEY idx_sensor_image_timestamp (`timestamp`)"
                    + ") COMMENT='sensor image data'",
            "CREATE TABLE IF NOT EXISTS sensor_api_endpoint ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "endpointCode VARCHAR(128) NOT NULL,"
                    + "name VARCHAR(256) NOT NULL,"
                    + "description VARCHAR(512) DEFAULT NULL,"
                    + "method VARCHAR(16) NOT NULL,"
                    + "internalPath VARCHAR(512) NOT NULL,"
                    + "targetService VARCHAR(64) NOT NULL,"
                    + "targetPath VARCHAR(512) NOT NULL,"
                    + "category VARCHAR(64) DEFAULT NULL,"
                    + "protocolType VARCHAR(32) NOT NULL DEFAULT 'http',"
                    + "requestSchema TEXT DEFAULT NULL,"
                    + "responseSchema TEXT DEFAULT NULL,"
                    + "status TINYINT NOT NULL DEFAULT 1,"
                    + "createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "isDelete TINYINT NOT NULL DEFAULT 0,"
                    + "UNIQUE KEY uk_sensor_api_endpoint_code (endpointCode),"
                    + "UNIQUE KEY uk_sensor_api_endpoint_path_method (internalPath, method)"
                    + ") COMMENT='sensor api endpoint'",
            "CREATE TABLE IF NOT EXISTS sensor_interface_binding ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "sensorApiEndpointId BIGINT NOT NULL,"
                    + "interfaceInfoId BIGINT NOT NULL,"
                    + "publishStrategy VARCHAR(32) NOT NULL DEFAULT 'proxy',"
                    + "authStrategy VARCHAR(32) NOT NULL DEFAULT 'gateway_aksk',"
                    + "limitStrategy VARCHAR(128) DEFAULT NULL,"
                    + "cacheStrategy VARCHAR(128) DEFAULT NULL,"
                    + "status TINYINT NOT NULL DEFAULT 1,"
                    + "createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "isDelete TINYINT NOT NULL DEFAULT 0,"
                    + "UNIQUE KEY uk_sensor_binding_endpoint_interface (sensorApiEndpointId, interfaceInfoId),"
                    + "KEY idx_sensor_binding_interface (interfaceInfoId)"
                    + ") COMMENT='sensor interface binding'",
            "CREATE TABLE IF NOT EXISTS sensor_realtime_channel ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "channelCode VARCHAR(128) NOT NULL,"
                    + "name VARCHAR(256) NOT NULL,"
                    + "wsPath VARCHAR(512) NOT NULL,"
                    + "topic VARCHAR(256) DEFAULT NULL,"
                    + "deviceType VARCHAR(64) DEFAULT NULL,"
                    + "authType VARCHAR(32) NOT NULL DEFAULT 'session',"
                    + "status TINYINT NOT NULL DEFAULT 1,"
                    + "createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "isDelete TINYINT NOT NULL DEFAULT 0,"
                    + "UNIQUE KEY uk_sensor_realtime_channel_code (channelCode)"
                    + ") COMMENT='sensor realtime channel'"
    };

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Value("${platform.public-base-url:http://localhost:7529/api}")
    private String platformPublicBaseUrl;

    @Override
    public void run(String... args) {
        for (String ddl : DDL_LIST) {
            jdbcTemplate.execute(ddl);
        }
        migrateDeprecatedSensorDeviceColumn();
        ensureSensorDeviceLegacyIdIndex();
        fillBlankLegacyDeviceIds();
        ensureGpsSourceRecordColumn();
        dropGpsDeviceTimeUniqueIndex();
        ensureGpsSourceRecordUniqueIndex();
        migrateDeprecatedEndpointCodes();
        migrateDeprecatedRealtimeChannelCodes();
        alignLocalNorthboundRoutes();
        alignManagedDeviceEndpointPaths();
        alignRealtimeChannelWsPath();
        migrateDeprecatedDefaultDataSource();
        alignDefaultDataSourceUrls();
        log.info("sensor registry schema is ready");
    }

    private void migrateDeprecatedSensorDeviceColumn() {
        Integer newColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = 'sensor_device' "
                        + "AND column_name = 'legacyDeviceId'",
                Integer.class
        );
        if (newColumnCount != null && newColumnCount > 0) {
            return;
        }
        Integer oldColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = 'sensor_device' "
                        + "AND column_name = 'sdsDeviceId'",
                Integer.class
        );
        try {
            if (oldColumnCount != null && oldColumnCount > 0) {
                jdbcTemplate.execute(
                        "ALTER TABLE sensor_device "
                                + "CHANGE COLUMN sdsDeviceId legacyDeviceId VARCHAR(128) DEFAULT NULL"
                );
                log.info("sensor_device.sdsDeviceId was renamed to legacyDeviceId");
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE sensor_device "
                            + "ADD COLUMN legacyDeviceId VARCHAR(128) DEFAULT NULL AFTER dataSourceId"
            );
            log.info("sensor_device.legacyDeviceId column is ready");
        } catch (Exception e) {
            log.warn("migrate sensor_device legacyDeviceId column failed: {}", e.getMessage());
        }
    }

    private void ensureSensorDeviceLegacyIdIndex() {
        Integer existingIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = 'sensor_device' "
                        + "AND index_name = 'idx_sensor_device_legacy_id'",
                Integer.class
        );
        if (existingIndexCount != null && existingIndexCount > 0) {
            return;
        }
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE sensor_device "
                            + "ADD KEY idx_sensor_device_legacy_id (legacyDeviceId)"
            );
            log.info("sensor_device legacyDeviceId index is ready");
        } catch (Exception e) {
            log.warn("ensure sensor_device legacyDeviceId index failed: {}", e.getMessage());
        }
    }

    private void fillBlankLegacyDeviceIds() {
        try {
            int updatedCount = jdbcTemplate.update(
                    "UPDATE sensor_device "
                            + "SET legacyDeviceId = deviceCode, updateTime = CURRENT_TIMESTAMP "
                            + "WHERE isDelete = 0 AND (legacyDeviceId IS NULL OR legacyDeviceId = '')"
            );
            if (updatedCount > 0) {
                log.info("filled {} blank legacyDeviceId values from deviceCode", updatedCount);
            }
        } catch (Exception e) {
            log.warn("fill blank legacyDeviceId values failed: {}", e.getMessage());
        }
    }

    private void ensureGpsSourceRecordColumn() {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = 'sensor_gps_data' "
                        + "AND column_name = 'sourceRecordId'",
                Integer.class
        );
        if (columnCount != null && columnCount > 0) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE sensor_gps_data ADD COLUMN sourceRecordId BIGINT DEFAULT NULL AFTER id");
            log.info("sensor_gps_data sourceRecordId column is ready");
        } catch (Exception e) {
            log.warn("ensure sensor_gps_data sourceRecordId column failed: {}", e.getMessage());
        }
    }

    private void dropGpsDeviceTimeUniqueIndex() {
        Integer existingIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = 'sensor_gps_data' "
                        + "AND index_name = 'uk_sensor_gps_data_device_time'",
                Integer.class
        );
        if (existingIndexCount != null && existingIndexCount > 0) {
            try {
                jdbcTemplate.execute("ALTER TABLE sensor_gps_data DROP INDEX uk_sensor_gps_data_device_time");
                log.info("sensor_gps_data deviceToken + timestampMillisecond unique key was removed");
            } catch (Exception e) {
                log.warn("drop sensor_gps_data deviceToken + timestampMillisecond unique key failed: {}", e.getMessage());
            }
        }
    }

    private void ensureGpsSourceRecordUniqueIndex() {
        Integer existingIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = 'sensor_gps_data' "
                        + "AND index_name = 'uk_sensor_gps_data_source_record'",
                Integer.class
        );
        if (existingIndexCount != null && existingIndexCount > 0) {
            return;
        }
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE sensor_gps_data "
                            + "ADD UNIQUE KEY uk_sensor_gps_data_source_record (sourceRecordId)"
            );
            log.info("sensor_gps_data sourceRecordId unique key is ready");
        } catch (Exception e) {
            log.warn("ensure sensor_gps_data sourceRecordId unique key failed: {}", e.getMessage());
        }
    }

    private void alignLocalNorthboundRoutes() {
        String placeholders = String.join(",", java.util.Collections.nCopies(LOCAL_PLATFORM_ENDPOINT_PATHS.length, "?"));
        List<Object> parameters = new ArrayList<>();
        parameters.add(DEFAULT_TARGET_SERVICE);
        parameters.addAll(Arrays.asList(LOCAL_PLATFORM_ENDPOINT_PATHS));
        int updatedCount = jdbcTemplate.update(
                "UPDATE sensor_api_endpoint "
                        + "SET targetService = ?, targetPath = internalPath, updateTime = CURRENT_TIMESTAMP "
                        + "WHERE isDelete = 0 AND internalPath IN (" + placeholders + ") "
                        + "AND (targetService <> ? OR targetPath IS NULL OR targetPath <> internalPath)",
                appendTailParameter(parameters, DEFAULT_TARGET_SERVICE)
        );
        if (updatedCount > 0) {
            log.info("aligned {} local northbound endpoint routes to {}", updatedCount, DEFAULT_TARGET_SERVICE);
        }
    }

    private void alignRealtimeChannelWsPath() {
        String wsUrl = buildDefaultWsUrl();
        int updatedCount = jdbcTemplate.update(
                "UPDATE sensor_realtime_channel "
                        + "SET wsPath = ?, updateTime = CURRENT_TIMESTAMP "
                        + "WHERE isDelete = 0 AND (wsPath IS NULL OR wsPath <> ?)",
                wsUrl,
                wsUrl
        );
        if (updatedCount > 0) {
            log.info("aligned {} realtime channel ws paths to {}", updatedCount, wsUrl);
        }
    }

    private void alignManagedDeviceEndpointPaths() {
        List<Map<String, Object>> deviceRows = jdbcTemplate.queryForList(
                "SELECT id, dataEndpoint FROM sensor_device WHERE isDelete = 0"
        );
        int updatedCount = 0;
        for (Map<String, Object> deviceRow : deviceRows) {
            Object idValue = deviceRow.get("id");
            if (!(idValue instanceof Number)) {
                continue;
            }
            String currentPath = deviceRow.get("dataEndpoint") == null
                    ? null
                    : String.valueOf(deviceRow.get("dataEndpoint"));
            if (StringUtils.isBlank(currentPath)) {
                continue;
            }
            String canonicalPath = SensorRouteAliasSupport.canonicalizeApiPath(currentPath);
            if (StringUtils.isBlank(canonicalPath) || StringUtils.equals(currentPath, canonicalPath)) {
                continue;
            }
            updatedCount += jdbcTemplate.update(
                    "UPDATE sensor_device "
                            + "SET dataEndpoint = ?, updateTime = CURRENT_TIMESTAMP "
                            + "WHERE id = ?",
                    canonicalPath,
                    ((Number) idValue).longValue()
            );
        }
        if (updatedCount > 0) {
            log.info("aligned {} managed device endpoints to canonical SensorHub routes", updatedCount);
        }
    }

    private void migrateDeprecatedEndpointCodes() {
        try {
            int updatedCount = jdbcTemplate.update(
                    "UPDATE sensor_api_endpoint AS current_endpoint "
                            + "SET current_endpoint.endpointCode = REPLACE(current_endpoint.endpointCode, ?, ?), "
                            + "current_endpoint.updateTime = CURRENT_TIMESTAMP "
                            + "WHERE current_endpoint.isDelete = 0 "
                            + "AND current_endpoint.endpointCode LIKE CONCAT(?, '%') "
                            + "AND NOT EXISTS ("
                            + "SELECT 1 FROM ("
                            + "SELECT endpointCode FROM sensor_api_endpoint WHERE isDelete = 0"
                            + ") AS existing_endpoint "
                            + "WHERE existing_endpoint.endpointCode = REPLACE(current_endpoint.endpointCode, ?, ?)"
                            + ")",
                    DEPRECATED_ENDPOINT_CODE_PREFIX,
                    CURRENT_ENDPOINT_CODE_PREFIX,
                    DEPRECATED_ENDPOINT_CODE_PREFIX,
                    DEPRECATED_ENDPOINT_CODE_PREFIX,
                    CURRENT_ENDPOINT_CODE_PREFIX
            );
            if (updatedCount > 0) {
                log.info("migrated {} deprecated endpoint codes to {}", updatedCount, CURRENT_ENDPOINT_CODE_PREFIX);
            }
        } catch (Exception e) {
            log.warn("migrate deprecated endpoint codes failed: {}", e.getMessage());
        }
    }

    private void migrateDeprecatedRealtimeChannelCodes() {
        try {
            int updatedCount = jdbcTemplate.update(
                    "UPDATE sensor_realtime_channel AS current_channel "
                            + "SET current_channel.channelCode = REPLACE(current_channel.channelCode, ?, ?), "
                            + "current_channel.updateTime = CURRENT_TIMESTAMP "
                            + "WHERE current_channel.isDelete = 0 "
                            + "AND current_channel.channelCode LIKE CONCAT(?, '%') "
                            + "AND NOT EXISTS ("
                            + "SELECT 1 FROM ("
                            + "SELECT channelCode FROM sensor_realtime_channel WHERE isDelete = 0"
                            + ") AS existing_channel "
                            + "WHERE existing_channel.channelCode = REPLACE(current_channel.channelCode, ?, ?)"
                            + ")",
                    DEPRECATED_CHANNEL_CODE_PREFIX,
                    CURRENT_CHANNEL_CODE_PREFIX,
                    DEPRECATED_CHANNEL_CODE_PREFIX,
                    DEPRECATED_CHANNEL_CODE_PREFIX,
                    CURRENT_CHANNEL_CODE_PREFIX
            );
            if (updatedCount > 0) {
                log.info("migrated {} deprecated realtime channel codes to {}", updatedCount, CURRENT_CHANNEL_CODE_PREFIX);
            }
        } catch (Exception e) {
            log.warn("migrate deprecated realtime channel codes failed: {}", e.getMessage());
        }
    }

    private void migrateDeprecatedDefaultDataSource() {
        try {
            Long currentSourceId = findActiveDataSourceIdByCode(DEFAULT_SOURCE_CODE);
            Long deprecatedSourceId = findActiveDataSourceIdByCode(DEPRECATED_SOURCE_CODE);
            if (deprecatedSourceId == null) {
                return;
            }
            if (currentSourceId != null) {
                int deviceUpdatedCount = jdbcTemplate.update(
                        "UPDATE sensor_device "
                                + "SET dataSourceId = ?, updateTime = CURRENT_TIMESTAMP "
                                + "WHERE isDelete = 0 AND dataSourceId = ?",
                        currentSourceId,
                        deprecatedSourceId
                );
                int archivedCount = jdbcTemplate.update(
                        "UPDATE sensor_data_source "
                                + "SET isDelete = 1, updateTime = CURRENT_TIMESTAMP "
                                + "WHERE id = ? AND isDelete = 0",
                        deprecatedSourceId
                );
                if (deviceUpdatedCount > 0 || archivedCount > 0) {
                    log.info("merged deprecated sensor data source {} into {}", DEPRECATED_SOURCE_CODE, DEFAULT_SOURCE_CODE);
                }
                return;
            }
            int updatedCount = jdbcTemplate.update(
                    "UPDATE sensor_data_source "
                            + "SET code = ?, name = ?, updateTime = CURRENT_TIMESTAMP "
                            + "WHERE id = ? AND isDelete = 0",
                    DEFAULT_SOURCE_CODE,
                    DEFAULT_SOURCE_NAME,
                    deprecatedSourceId
            );
            if (updatedCount > 0) {
                log.info("migrated deprecated sensor data source code from {} to {}", DEPRECATED_SOURCE_CODE, DEFAULT_SOURCE_CODE);
            }
        } catch (Exception e) {
            log.warn("migrate deprecated sensor data source failed: {}", e.getMessage());
        }
    }

    private Long findActiveDataSourceIdByCode(String code) {
        List<Long> idList = jdbcTemplate.queryForList(
                "SELECT id FROM sensor_data_source WHERE code = ? AND isDelete = 0 ORDER BY id ASC",
                Long.class,
                code
        );
        return idList.isEmpty() ? null : idList.get(0);
    }

    private void alignDefaultDataSourceUrls() {
        String wsUrl = buildDefaultWsUrl();
        int updatedCount = jdbcTemplate.update(
                "UPDATE sensor_data_source "
                        + "SET name = ?, baseUrl = ?, wsUrl = ?, updateTime = CURRENT_TIMESTAMP "
                        + "WHERE code = ? AND isDelete = 0 "
                        + "AND (name <> ? OR baseUrl IS NULL OR baseUrl <> ? OR wsUrl IS NULL OR wsUrl <> ?)",
                DEFAULT_SOURCE_NAME,
                platformPublicBaseUrl,
                wsUrl,
                DEFAULT_SOURCE_CODE,
                DEFAULT_SOURCE_NAME,
                platformPublicBaseUrl,
                wsUrl
        );
        if (updatedCount > 0) {
            log.info("aligned default sensor data source urls to local platform");
        }
    }

    private Object[] appendTailParameter(List<Object> parameters, Object tailParameter) {
        List<Object> allParameters = new ArrayList<>(parameters);
        allParameters.add(tailParameter);
        return allParameters.toArray();
    }

    private String buildDefaultWsUrl() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(
                StringUtils.defaultIfBlank(platformPublicBaseUrl, "http://localhost:7529/api")
        );
        String host = builder.build().getHost();
        int port = builder.build().getPort();
        String path = StringUtils.defaultIfBlank(builder.build().getPath(), "");
        if (StringUtils.isBlank(host)) {
            return "ws://localhost:7529/api/ws/sensor/realtime";
        }
        StringBuilder wsBuilder = new StringBuilder("ws://").append(host);
        if (port > 0) {
            wsBuilder.append(":").append(port);
        }
        if (StringUtils.isNotBlank(path)) {
            wsBuilder.append(StringUtils.removeEnd(path, "/"));
        }
        wsBuilder.append("/ws/sensor/realtime");
        return wsBuilder.toString();
    }
}
