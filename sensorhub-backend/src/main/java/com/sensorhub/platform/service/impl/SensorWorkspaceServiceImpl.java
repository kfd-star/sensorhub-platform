package com.sensorhub.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.sensorhub.platform.common.ErrorCode;
import com.sensorhub.platform.constant.UserConstant;
import com.sensorhub.platform.exception.BusinessException;
import com.sensorhub.platform.integration.workspace.SensorWorkspaceInterfaceDefinition;
import com.sensorhub.platform.model.enums.InterfaceInfoStatusEnum;
import com.sensorhub.platform.model.entity.SensorApiEndpoint;
import com.sensorhub.platform.model.entity.SensorDataSource;
import com.sensorhub.platform.model.entity.SensorDevice;
import com.sensorhub.platform.model.entity.SensorRealtimeChannel;
import com.sensorhub.platform.model.vo.SensorWorkspaceCatalogSyncResultVO;
import com.sensorhub.platform.model.vo.SensorWorkspaceInterfaceVO;
import com.sensorhub.platform.service.InterfaceInfoService;
import com.sensorhub.platform.service.SensorWorkspaceService;
import com.sensorhub.platform.service.SensorApiEndpointService;
import com.sensorhub.platform.service.SensorDataSourceService;
import com.sensorhub.platform.service.SensorDeviceService;
import com.sensorhub.platform.service.SensorNorthboundService;
import com.sensorhub.platform.service.SensorRealtimeChannelService;
import com.sensorhub.platform.service.UserService;
import com.sensorhub.common.model.entity.InterfaceInfo;
import com.sensorhub.common.support.SensorRouteAliasSupport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Sensor workspace aggregation service.
 */
@Service
public class SensorWorkspaceServiceImpl implements SensorWorkspaceService {

    private static final String PLATFORM_HOST = "http://localhost:8123";
    private static final int DEFAULT_REALTIME_LIMIT = 5;
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final String DEFAULT_SOURCE_CODE = "API_EXAMPLE_LOCAL";
    private static final String DEFAULT_SOURCE_NAME = "SensorHub Local Data Source";

    private static final String COMMON_REQUEST_HEADER = "["
            + "{\"name\":\"accessKey\",\"required\":true,\"description\":\"platform access key\"},"
            + "{\"name\":\"nonce\",\"required\":true,\"description\":\"request nonce\"},"
            + "{\"name\":\"timestamp\",\"required\":true,\"description\":\"unix timestamp\"},"
            + "{\"name\":\"sign\",\"required\":true,\"description\":\"gateway signature\"},"
            + "{\"name\":\"body\",\"required\":true,\"description\":\"request body used in signing\"}"
            + "]";

    private static final String JSON_RESPONSE_HEADER = "["
            + "{\"name\":\"Content-Type\",\"value\":\"application/json\"}"
            + "]";

    private static final List<SensorDatasetDefinition> DATASET_DEFINITIONS = Arrays.asList(
            new SensorDatasetDefinition("gps", "GPS Data", "/api/sensor/gps-data", "/api/sensor/gps-data", "gps"),
            new SensorDatasetDefinition("esp32Weather", "ESP32 Weather", "/api/sensor/esp32-weather-data", "/api/sensor/esp32-weather-data", "ESP32_weather"),
            new SensorDatasetDefinition("weatherStation1", "Weather Station 1", "/api/sensor/weather-station-1-data", "/api/sensor/weather-station-1-data", "weather_station_1"),
            new SensorDatasetDefinition("weatherStation2", "Weather Station 2", "/api/sensor/weather-station-2-data", "/api/sensor/weather-station-2-data", "weather_station_2"),
            new SensorDatasetDefinition("wind", "Wind Data", "/api/sensor/wind-data", "/api/sensor/wind-data", "wind"),
            new SensorDatasetDefinition("windOcr", "Wind OCR", "/api/sensor/wind-ocr-data", "/api/sensor/wind-ocr-data", "wind_ocr"),
            new SensorDatasetDefinition("image", "Image Data", "/api/sensor/image-data", "/api/sensor/image-data", "image")
    );

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Resource
    private SensorDataSourceService sensorDataSourceService;

    @Resource
    private SensorApiEndpointService sensorApiEndpointService;

    @Resource
    private SensorDeviceService sensorDeviceService;

    @Resource
    private SensorRealtimeChannelService sensorRealtimeChannelService;

    @Resource
    private SensorNorthboundService sensorNorthboundService;

    @Resource
    private UserService userService;

    @Value("${platform.public-base-url:http://localhost:7529/api}")
    private String platformPublicBaseUrl;

    private final Gson gson = new Gson();

    @Override
    public List<SensorWorkspaceInterfaceVO> listNorthboundInterfaces() {
        List<SensorWorkspaceInterfaceDefinition> catalog = buildCatalog();
        Map<String, InterfaceInfo> existingMap = getExistingInterfaceMap(catalog);
        return catalog.stream()
                .map(item -> toVO(item, existingMap.get(buildKey(item.getPlatformUrl(), item.getMethod()))))
                .collect(Collectors.toList());
    }

    @Override
    public SensorWorkspaceCatalogSyncResultVO syncNorthboundInterfaces() {
        List<SensorWorkspaceInterfaceDefinition> catalog = buildCatalog();
        Map<String, InterfaceInfo> existingMap = getExistingInterfaceMap(catalog);

        List<String> createdNames = new ArrayList<>();
        List<String> updatedNames = new ArrayList<>();

        for (SensorWorkspaceInterfaceDefinition item : catalog) {
            String key = buildKey(item.getPlatformUrl(), item.getMethod());
            InterfaceInfo existing = existingMap.get(key);
            if (existing == null) {
                InterfaceInfo interfaceInfo = new InterfaceInfo();
                fillInterfaceInfo(item, interfaceInfo);
                interfaceInfo.setStatus(InterfaceInfoStatusEnum.ONLINE.getValue());
                interfaceInfo.setUserId(UserConstant.SYSTEM_USER_ID);
                interfaceInfoService.save(interfaceInfo);
                createdNames.add(item.getName());
                continue;
            }

            fillInterfaceInfo(item, existing);
            existing.setStatus(InterfaceInfoStatusEnum.ONLINE.getValue());
            interfaceInfoService.updateById(existing);
            updatedNames.add(item.getName());
        }

        SensorWorkspaceCatalogSyncResultVO result = new SensorWorkspaceCatalogSyncResultVO();
        result.setTotal(catalog.size());
        result.setCreatedCount(createdNames.size());
        result.setUpdatedCount(updatedNames.size());
        result.setCreatedNames(createdNames);
        result.setUpdatedNames(updatedNames);
        return result;
    }

    @Override
    public Map<String, Object> getWorkspaceOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        List<SensorWorkspaceInterfaceVO> catalog = listNorthboundInterfaces();
        long syncedCount = catalog.stream().filter(SensorWorkspaceInterfaceVO::getSynced).count();
        long publishedCount = catalog.stream()
                .filter(item -> item.getStatus() != null
                        && item.getStatus().intValue() == InterfaceInfoStatusEnum.ONLINE.getValue())
                .count();

        overview.put("catalogTotal", catalog.size());
        overview.put("catalogSynced", syncedCount);
        overview.put("catalogPublished", publishedCount);
        overview.put("gatewayBaseUrl", PLATFORM_HOST);
        overview.put("datasets", DATASET_DEFINITIONS.stream().map(this::toDatasetMap).collect(Collectors.toList()));
        overview.put("realtimeWsUrl", resolveRealtimeWsUrl());
        overview.put("runtimeMode", "local-only");
        overview.put("queryService", "sensorNorthboundService");
        overview.put("compatibilityBridge", "none");

        List<Map<String, Object>> devices = listSensorDevices();
        List<Map<String, Object>> channels = listRealtimeChannels();
        Map<String, Object> localStatsResponse = sensorNorthboundService.getDataStats(null, null);
        overview.put("dataStats", extractTopLevelMap(localStatsResponse.get("data")));
        overview.put("deviceCount", devices.size());
        overview.put("enabledDeviceCount", devices.stream().filter(this::isEnabledDevice).count());
        overview.put("devices", devices.stream().limit(5).collect(Collectors.toList()));
        overview.put("realtimeChannelCount", channels.size());

        Map<String, Object> databaseDebugInfo = sensorNorthboundService.getDatabaseDebugInfo();
        boolean localPlatformReady = isSuccessResponse(databaseDebugInfo);
        overview.put("platformReady", localPlatformReady);
        overview.put("workspaceReady", localPlatformReady);
        overview.put("platformError", localPlatformReady ? null : String.valueOf(databaseDebugInfo.get("message")));
        overview.put("downstreamError", overview.get("platformError"));
        overview.put(
                "health",
                buildLocalOverviewHealth(databaseDebugInfo, localPlatformReady, devices.size(), channels.size())
        );
        overview.put(
                "systemStatus",
                buildLocalOverviewSystemStatus(
                        databaseDebugInfo,
                        catalog.size(),
                        publishedCount,
                        devices.size(),
                        channels.size()
                )
        );
        return overview;
    }

    private Map<String, Object> buildLocalOverviewHealth(Map<String, Object> databaseDebugInfo,
                                                         boolean localPlatformReady,
                                                         int deviceCount,
                                                         int realtimeChannelCount) {
        Map<String, Object> health = new LinkedHashMap<>();
        Map<String, Object> database = extractTopLevelMap(databaseDebugInfo.get("database"));
        Map<String, Object> mysqlRuntime = extractTopLevelMap(databaseDebugInfo.get("mysqlRuntime"));
        health.put("status", localPlatformReady ? "UP" : "DEGRADED");
        health.put("database", buildDatabaseSummary(database, mysqlRuntime));
        health.put("mode", "sensorhub-local-overview");
        health.put("service", "sensorhub-backend");
        health.put("deviceCount", deviceCount);
        health.put("realtimeChannelCount", realtimeChannelCount);
        health.put("checkedAt", Instant.now().toString());
        health.put("workspaceRequired", false);
        return health;
    }

    private Map<String, Object> buildLocalOverviewSystemStatus(Map<String, Object> databaseDebugInfo,
                                                               int apiEndpointCount,
                                                               long publishedInterfaceCount,
                                                               int deviceCount,
                                                               int realtimeChannelCount) {
        Map<String, Object> systemStatus = new LinkedHashMap<>();
        Map<String, Object> database = extractTopLevelMap(databaseDebugInfo.get("database"));
        Map<String, Object> mysqlRuntime = extractTopLevelMap(databaseDebugInfo.get("mysqlRuntime"));
        Map<String, Object> tableCounts = extractTopLevelMap(databaseDebugInfo.get("tableCounts"));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("api_endpoints", apiEndpointCount);
        stats.put("published_interfaces", publishedInterfaceCount);
        stats.put("users", userService.count());
        stats.put("devices", deviceCount);
        stats.put("realtime_channels", realtimeChannelCount);
        stats.put("sensor_tables", countNonEmptyValues(tableCounts));

        systemStatus.put("database", buildDatabaseSummary(database, mysqlRuntime));
        systemStatus.put("stats", stats);
        systemStatus.put("tableCounts", tableCounts);
        systemStatus.put("mysqlRuntime", mysqlRuntime);
        systemStatus.put("runtimeMode", "local-only");
        systemStatus.put("compatibilityBridge", "none");
        systemStatus.put("generatedAt", Instant.now().toString());
        return systemStatus;
    }

    private String buildDatabaseSummary(Map<String, Object> database, Map<String, Object> mysqlRuntime) {
        String product = getStringValue(database, "product");
        String databaseName = getStringValue(mysqlRuntime, "database");
        String version = getStringValue(mysqlRuntime, "version");
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotBlank(product)) {
            builder.append(product);
        }
        if (StringUtils.isNotBlank(databaseName)) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(databaseName);
        }
        if (StringUtils.isNotBlank(version)) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(version);
        }
        return builder.length() == 0 ? "local-mysql" : builder.toString();
    }

    private int countNonEmptyValues(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Object value : source.values()) {
            if (value instanceof Number) {
                if (((Number) value).longValue() >= 0) {
                    count++;
                }
            } else if (value != null) {
                count++;
            }
        }
        return count;
    }

    private boolean isSuccessResponse(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return false;
        }
        Object success = response.get("success");
        if (success instanceof Boolean) {
            return (Boolean) success;
        }
        if (success instanceof String) {
            return Boolean.parseBoolean((String) success);
        }
        return false;
    }

    @Override
    public List<Map<String, Object>> listSensorDevices() {
        List<SensorDevice> managedDeviceList = sensorDeviceService.list(
                new QueryWrapper<SensorDevice>().orderByDesc("updateTime")
        );
        return managedDeviceList.stream().map(this::toManagedDeviceMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getRealtimeWorkspace() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> devices = listSensorDevices();
        List<Map<String, Object>> channels = listRealtimeChannels();
        result.put("wsUrl", resolveRealtimeWsUrl());
        result.put("channels", channels);
        result.put("channelCount", channels.size());
        result.put("devices", devices);
        result.put("deviceCount", devices.size());
        result.put("enabledDeviceCount", devices.stream().filter(this::isEnabledDevice).count());
        result.put("generatedAt", Instant.now().toString());
        return result;
    }

    @Override
    public Map<String, Object> createSensorDevice(Map<String, Object> devicePayload) {
        SensorDevice sensorDevice = saveManagedDevice(devicePayload, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "device created in sensor registry");
        result.put("device", toManagedDeviceMap(sensorDevice));
        return result;
    }

    @Override
    public Map<String, Object> updateSensorDevice(String deviceId, Map<String, Object> devicePayload) {
        if (StringUtils.isBlank(deviceId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "deviceId is required");
        }
        SensorDevice existingDevice = findManagedDevice(deviceId);
        SensorDevice sensorDevice = saveManagedDevice(devicePayload, existingDevice);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "device updated in sensor registry");
        result.put("device", toManagedDeviceMap(sensorDevice));
        return result;
    }

    @Override
    public Map<String, Object> deleteSensorDevice(String deviceId) {
        if (StringUtils.isBlank(deviceId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "deviceId is required");
        }
        SensorDevice existingDevice = findManagedDevice(deviceId);
        boolean removed = sensorDeviceService.removeById(existingDevice.getId());
        if (removed && StringUtils.isNotBlank(existingDevice.getRealtimeKey())) {
            sensorRealtimeChannelService.remove(
                    new QueryWrapper<SensorRealtimeChannel>()
                            .eq("channelCode", buildManagedRealtimeChannelCode(existingDevice.getRealtimeKey()))
            );
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", removed ? "device removed from sensor registry" : "device remove failed");
        result.put("deleted", removed);
        result.put("deviceId", deviceId);
        return result;
    }

    @Override
    public Map<String, Object> testSensorDevice(Map<String, Object> devicePayload) {
        String dataEndpoint = getStringValue(devicePayload, "data_endpoint");
        String deviceToken = getStringValue(devicePayload, "token");
        if (StringUtils.isBlank(dataEndpoint)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "data_endpoint is required");
        }
        String normalizedPath = normalizeTargetPath(dataEndpoint);
        Map<String, Object> response = queryDatasetByPath(normalizedPath, 1, 0, deviceToken, null, null);
        if (response == null) {
            throw new BusinessException(
                    ErrorCode.PARAMS_ERROR,
                    "unsupported local sensor dataset path: " + normalizedPath
            );
        }
        List<Map<String, Object>> records = extractDataList(response);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targetPath", StringUtils.defaultIfBlank(normalizeManagedDataEndpoint(dataEndpoint), normalizedPath));
        result.put("count", extractCount(response, records.size()));
        result.put("sample", records.isEmpty() ? null : records.get(0));
        result.put("deviceToken", deviceToken);
        result.put("queryMode", "sensorNorthbound-local-only");
        result.put("message", records.isEmpty() ? "query succeeded but no data returned" : "device query is reachable");
        return result;
    }

    @Override
    public Map<String, Object> getRealtimeSnapshot(Integer limit) {
        int safeLimit = normalizeLimit(limit, DEFAULT_REALTIME_LIMIT);
        List<Map<String, Object>> datasets = new ArrayList<>();

        for (SensorDatasetDefinition datasetDefinition : DATASET_DEFINITIONS) {
            Map<String, Object> datasetResult = toDatasetMap(datasetDefinition);
            try {
                Map<String, Object> response = queryDataset(datasetDefinition, safeLimit, 0, null, null, null);
                List<Map<String, Object>> records = extractDataList(response);
                datasetResult.put("available", true);
                datasetResult.put("count", extractCount(response, records.size()));
                datasetResult.put("records", records);
                datasetResult.put("error", null);
                datasetResult.put("latestRecord", records.isEmpty() ? null : records.get(0));
                datasetResult.put("queryMode", "sensorNorthbound-local-only");
            } catch (BusinessException e) {
                datasetResult.put("available", false);
                datasetResult.put("count", 0);
                datasetResult.put("records", Collections.emptyList());
                datasetResult.put("error", e.getMessage());
                datasetResult.put("latestRecord", null);
            }
            datasets.add(datasetResult);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedAt", Instant.now().toString());
        result.put("limit", safeLimit);
        result.put("datasets", datasets);
        return result;
    }

    @Override
    public Map<String, Object> searchDataset(
            String datasetCode,
            String deviceToken,
            String startDate,
            String endDate,
            Integer limit
    ) {
        SensorDatasetDefinition datasetDefinition = getDatasetDefinition(datasetCode);
        int safeLimit = normalizeLimit(limit, DEFAULT_SEARCH_LIMIT);
        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("limit", safeLimit);
        if (StringUtils.isNotBlank(deviceToken)) {
            queryParams.put("device_token", deviceToken.trim());
        }
        if (StringUtils.isNotBlank(startDate)) {
            queryParams.put("startDate", startDate.trim());
        }
        if (StringUtils.isNotBlank(endDate)) {
            queryParams.put("endDate", endDate.trim());
        }

        Map<String, Object> response = queryDataset(datasetDefinition, safeLimit, 0, deviceToken, startDate, endDate);
        List<Map<String, Object>> records = extractDataList(response);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataset", datasetDefinition.getCode());
        result.put("datasetName", datasetDefinition.getName());
        result.put("gatewayPath", datasetDefinition.getGatewayPath());
        result.put("count", extractCount(response, records.size()));
        result.put("records", records);
        result.put("query", new LinkedHashMap<>(queryParams));
        result.put("queryMode", "sensorNorthbound-local-only");
        return result;
    }

    private Map<String, Object> queryDataset(SensorDatasetDefinition datasetDefinition,
                                             Integer limit,
                                             Integer offset,
                                             String deviceToken,
                                             String startDate,
                                             String endDate) {
        String datasetCode = datasetDefinition.getCode();
        if ("gps".equalsIgnoreCase(datasetCode)) {
            return sensorNorthboundService.listGpsData(limit, offset, deviceToken, startDate, endDate);
        }
        if ("esp32Weather".equalsIgnoreCase(datasetCode)) {
            return sensorNorthboundService.listEsp32WeatherData(limit, offset, deviceToken, startDate, endDate);
        }
        if ("weatherStation1".equalsIgnoreCase(datasetCode)) {
            return sensorNorthboundService.listWeatherStation1Data(limit, offset, deviceToken, startDate, endDate);
        }
        if ("weatherStation2".equalsIgnoreCase(datasetCode)) {
            return sensorNorthboundService.listWeatherStation2Data(limit, offset, deviceToken, startDate, endDate);
        }
        if ("wind".equalsIgnoreCase(datasetCode)) {
            return sensorNorthboundService.listWindData(limit, offset, deviceToken, startDate, endDate);
        }
        if ("windOcr".equalsIgnoreCase(datasetCode)) {
            return sensorNorthboundService.listWindOcrData(limit, offset, deviceToken, startDate, endDate);
        }
        if ("image".equalsIgnoreCase(datasetCode)) {
            return sensorNorthboundService.listImageData(limit, offset, deviceToken, startDate, endDate);
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "unsupported sensor dataset");
    }

    private Map<String, Object> queryDatasetByPath(String path,
                                                   Integer limit,
                                                   Integer offset,
                                                   String deviceToken,
                                                   String startDate,
                                                   String endDate) {
        SensorDatasetDefinition datasetDefinition = getDatasetDefinitionByPath(path);
        if (datasetDefinition == null) {
            return null;
        }
        return queryDataset(datasetDefinition, limit, offset, deviceToken, startDate, endDate);
    }

    private void fillInterfaceInfo(SensorWorkspaceInterfaceDefinition item, InterfaceInfo interfaceInfo) {
        interfaceInfo.setName(item.getName());
        interfaceInfo.setDescription(item.getDescription());
        interfaceInfo.setMethod(item.getMethod());
        interfaceInfo.setUrl(item.getPlatformUrl());
        interfaceInfo.setRequestParams(item.getRequestParams());
        interfaceInfo.setRequestHeader(item.getRequestHeader());
        interfaceInfo.setResponseHeader(item.getResponseHeader());
    }

    private Map<String, InterfaceInfo> getExistingInterfaceMap(List<SensorWorkspaceInterfaceDefinition> catalog) {
        if (catalog.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> urls = catalog.stream()
                .map(SensorWorkspaceInterfaceDefinition::getPlatformUrl)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (urls.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("url", urls);
        List<InterfaceInfo> interfaceInfoList = interfaceInfoService.list(queryWrapper);
        Map<String, InterfaceInfo> result = new HashMap<>();
        for (InterfaceInfo interfaceInfo : interfaceInfoList) {
            result.put(buildKey(interfaceInfo.getUrl(), interfaceInfo.getMethod()), interfaceInfo);
        }
        return result;
    }

    private SensorWorkspaceInterfaceVO toVO(SensorWorkspaceInterfaceDefinition item, InterfaceInfo interfaceInfo) {
        SensorWorkspaceInterfaceVO vo = new SensorWorkspaceInterfaceVO();
        vo.setName(item.getName());
        vo.setDescription(item.getDescription());
        vo.setMethod(item.getMethod());
        vo.setGatewayPath(item.getGatewayPath());
        vo.setPlatformUrl(item.getPlatformUrl());
        vo.setTargetPath(item.getTargetPath());
        vo.setRequestParams(item.getRequestParams());
        vo.setRequestHeader(item.getRequestHeader());
        vo.setResponseHeader(item.getResponseHeader());
        vo.setSynced(interfaceInfo != null);
        if (interfaceInfo != null) {
            vo.setInterfaceInfoId(interfaceInfo.getId());
            vo.setStatus(interfaceInfo.getStatus());
        }
        return vo;
    }

    private String buildKey(String url, String method) {
        return method + "#" + url;
    }

    private List<SensorWorkspaceInterfaceDefinition> buildCatalog() {
        List<SensorApiEndpoint> localEndpointList = sensorApiEndpointService.list(
                new QueryWrapper<SensorApiEndpoint>().orderByAsc("id")
        );
        if (!localEndpointList.isEmpty()) {
            List<SensorWorkspaceInterfaceDefinition> localCatalog = localEndpointList.stream()
                    .map(this::toLocalDefinition)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            appendMissingDeviceRegistry(localCatalog);
            return localCatalog;
        }
        return buildLegacyCatalog();
    }

    private List<SensorWorkspaceInterfaceDefinition> buildLegacyCatalog() {
        return Arrays.asList(
                definition(
                        "Sensor Devices",
                        "Query the unified sensor registry managed by SensorHub Open Platform.",
                        "/api/sensor/devices",
                        "/api/sensor/devices",
                        "[]"
                ),
                definition(
                        "GPS Data Query",
                        "Query SensorHub local GPS telemetry data.",
                        "/api/sensor/gps-data",
                        "/api/sensor/gps-data",
                        commonDataQueryParams()
                ),
                definition(
                        "ESP32 Weather Query",
                        "Query SensorHub local ESP32 weather telemetry data.",
                        "/api/sensor/esp32-weather-data",
                        "/api/sensor/esp32-weather-data",
                        commonDataQueryParams()
                ),
                definition(
                        "Weather Station 1 Query",
                        "Query SensorHub local weather station 1 telemetry data.",
                        "/api/sensor/weather-station-1-data",
                        "/api/sensor/weather-station-1-data",
                        commonDataQueryParams()
                ),
                definition(
                        "Weather Station 2 Query",
                        "Query SensorHub local weather station 2 telemetry data.",
                        "/api/sensor/weather-station-2-data",
                        "/api/sensor/weather-station-2-data",
                        commonDataQueryParams()
                ),
                definition(
                        "Wind Data Query",
                        "Query SensorHub local wind telemetry data.",
                        "/api/sensor/wind-data",
                        "/api/sensor/wind-data",
                        commonDataQueryParams()
                ),
                definition(
                        "Wind OCR Query",
                        "Query SensorHub local wind OCR telemetry data.",
                        "/api/sensor/wind-ocr-data",
                        "/api/sensor/wind-ocr-data",
                        commonDataQueryParams()
                ),
                definition(
                        "Image Data Query",
                        "Query SensorHub local image and detection data.",
                        "/api/sensor/image-data",
                        "/api/sensor/image-data",
                        commonDataQueryParams()
                ),
                definition(
                        "Sensor Data Stats",
                        "Query SensorHub local sensor statistics.",
                        "/api/sensor/data-stats",
                        "/api/sensor/data-stats",
                        "["
                                + "{\"name\":\"startDate\",\"type\":\"string\",\"required\":false,\"description\":\"yyyy-MM-dd\"},"
                                + "{\"name\":\"endDate\",\"type\":\"string\",\"required\":false,\"description\":\"yyyy-MM-dd\"}"
                                + "]"
                )
        );
    }

    private SensorWorkspaceInterfaceDefinition toDynamicDefinition(Map<String, Object> endpointMap) {
        String targetPath = getStringValue(endpointMap, "path");
        String method = StringUtils.upperCase(getStringValue(endpointMap, "method"));
        if (StringUtils.isAnyBlank(targetPath, method)) {
            return null;
        }
        String gatewayPath = resolveGatewayPath(targetPath);
        String requestParams = getStringValue(endpointMap, "parameters");
        String requestHeader = buildRequestHeader(getStringValue(endpointMap, "headers"));
        String responseHeader = StringUtils.defaultIfBlank(
                getStringValue(endpointMap, "response_format"),
                JSON_RESPONSE_HEADER
        );
        return new SensorWorkspaceInterfaceDefinition(
                StringUtils.equals(targetPath, "/api/devices")
                        ? "Sensor Devices"
                        : StringUtils.defaultIfBlank(getStringValue(endpointMap, "name"), method + " " + targetPath),
                StringUtils.equals(targetPath, "/api/devices")
                        ? "Query the unified sensor registry managed by SensorHub Open Platform."
                        : StringUtils.defaultIfBlank(getStringValue(endpointMap, "description"), "Sensor workspace api endpoint"),
                method,
                gatewayPath,
                PLATFORM_HOST + gatewayPath,
                targetPath,
                StringUtils.defaultIfBlank(requestParams, "[]"),
                requestHeader,
                responseHeader
        );
    }

    private SensorWorkspaceInterfaceDefinition toLocalDefinition(SensorApiEndpoint endpoint) {
        if (endpoint == null || StringUtils.isBlank(endpoint.getMethod())) {
            return null;
        }
        String gatewayPath = StringUtils.defaultIfBlank(
                StringUtils.trimToNull(endpoint.getInternalPath()),
                resolveGatewayPath(endpoint.getTargetPath())
        );
        String targetPath = StringUtils.defaultIfBlank(
                StringUtils.trimToNull(endpoint.getTargetPath()),
                gatewayPath
        );
        return new SensorWorkspaceInterfaceDefinition(
                StringUtils.defaultIfBlank(endpoint.getName(), endpoint.getEndpointCode()),
                StringUtils.defaultIfBlank(endpoint.getDescription(), "Sensor registry managed api endpoint"),
                StringUtils.upperCase(endpoint.getMethod()),
                gatewayPath,
                PLATFORM_HOST + gatewayPath,
                targetPath,
                StringUtils.defaultIfBlank(endpoint.getRequestSchema(), "[]"),
                COMMON_REQUEST_HEADER,
                StringUtils.defaultIfBlank(endpoint.getResponseSchema(), JSON_RESPONSE_HEADER)
        );
    }

    private SensorWorkspaceInterfaceDefinition definition(
            String name,
            String description,
            String gatewayPath,
            String targetPath,
            String requestParams
    ) {
        return new SensorWorkspaceInterfaceDefinition(
                name,
                description,
                "GET",
                gatewayPath,
                PLATFORM_HOST + gatewayPath,
                targetPath,
                requestParams,
                COMMON_REQUEST_HEADER,
                JSON_RESPONSE_HEADER
        );
    }

    private String resolveGatewayPath(String targetPath) {
        return SensorRouteAliasSupport.canonicalizeApiPath(targetPath);
    }

    private String buildRequestHeader(String endpointHeaders) {
        if (StringUtils.isBlank(endpointHeaders)) {
            return COMMON_REQUEST_HEADER;
        }
        return endpointHeaders;
    }

    private void appendMissingDeviceRegistry(List<SensorWorkspaceInterfaceDefinition> catalog) {
        boolean hasDeviceRegistry = catalog.stream()
                .anyMatch(item -> StringUtils.equalsAny(
                        item.getTargetPath(),
                        "/api/devices",
                        "/api/sensor/devices"
                ) || StringUtils.equals(item.getGatewayPath(), "/api/sensor/devices"));
        if (hasDeviceRegistry) {
            return;
        }
        catalog.add(0, definition(
                "Sensor Devices",
                "Query the unified sensor registry managed by SensorHub Open Platform.",
                "/api/sensor/devices",
                "/api/sensor/devices",
                "[]"
        ));
    }

    private String commonDataQueryParams() {
        return "["
                + "{\"name\":\"limit\",\"type\":\"number\",\"required\":false,\"description\":\"query size\"},"
                + "{\"name\":\"offset\",\"type\":\"number\",\"required\":false,\"description\":\"query offset\"},"
                + "{\"name\":\"device_token\",\"type\":\"string\",\"required\":false,\"description\":\"device token\"},"
                + "{\"name\":\"startDate\",\"type\":\"string\",\"required\":false,\"description\":\"yyyy-MM-dd\"},"
                + "{\"name\":\"endDate\",\"type\":\"string\",\"required\":false,\"description\":\"yyyy-MM-dd\"}"
                + "]";
    }

    private List<Map<String, Object>> extractDataList(Map<String, Object> response) {
        Object data = response.get("data");
        if (data == null) {
            return Collections.emptyList();
        }
        JsonElement jsonElement = gson.toJsonTree(data);
        Type listType = new TypeToken<List<Map<String, Object>>>() {
        }.getType();
        List<Map<String, Object>> records = gson.fromJson(jsonElement, listType);
        return records == null ? Collections.emptyList() : records;
    }

    private Map<String, Object> extractDataMap(Map<String, Object> response) {
        Object data = response.get("data");
        if (data == null) {
            return Collections.emptyMap();
        }
        JsonElement jsonElement = gson.toJsonTree(data);
        Type mapType = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> result = gson.fromJson(jsonElement, mapType);
        return result == null ? Collections.emptyMap() : result;
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

    private int extractCount(Map<String, Object> response, int defaultValue) {
        Object count = response.get("count");
        if (count instanceof Number) {
            return ((Number) count).intValue();
        }
        if (count instanceof String && StringUtils.isNumeric((String) count)) {
            return Integer.parseInt((String) count);
        }
        return defaultValue;
    }

    private int normalizeLimit(Integer limit, int defaultValue) {
        if (limit == null || limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, 100);
    }

    private boolean isEnabledDevice(Map<String, Object> device) {
        Object enabled = device.get("enabled");
        if (enabled instanceof Boolean) {
            return (Boolean) enabled;
        }
        if (enabled instanceof String) {
            return Boolean.parseBoolean((String) enabled);
        }
        return false;
    }

    private List<Map<String, Object>> listRealtimeChannels() {
        List<SensorRealtimeChannel> channelList = sensorRealtimeChannelService.list();
        if (!channelList.isEmpty()) {
            return channelList.stream().map(this::toRealtimeChannelMap).collect(Collectors.toList());
        }
        return buildFallbackRealtimeChannels(listSensorDevices());
    }

    private SensorDatasetDefinition getDatasetDefinition(String datasetCode) {
        return DATASET_DEFINITIONS.stream()
                .filter(item -> item.getCode().equalsIgnoreCase(datasetCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "unsupported sensor dataset"));
    }

    private SensorDatasetDefinition getDatasetDefinitionByPath(String path) {
        String normalizedPath = normalizeDatasetLookupPath(path);
        return DATASET_DEFINITIONS.stream()
                .filter(item -> normalizedPath.equalsIgnoreCase(normalizeDatasetLookupPath(item.getTargetPath()))
                        || normalizedPath.equalsIgnoreCase(normalizeDatasetLookupPath(item.getGatewayPath())))
                .findFirst()
                .orElse(null);
    }

    private String getStringValue(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private Map<String, Object> toDatasetMap(SensorDatasetDefinition datasetDefinition) {
        Map<String, Object> dataset = new LinkedHashMap<>();
        dataset.put("code", datasetDefinition.getCode());
        dataset.put("name", datasetDefinition.getName());
        dataset.put("gatewayPath", datasetDefinition.getGatewayPath());
        dataset.put("targetPath", datasetDefinition.getTargetPath());
        dataset.put("statsKey", datasetDefinition.getStatsKey());
        return dataset;
    }

    private String resolveRealtimeWsUrl() {
        return buildPlatformRealtimeWsUrl();
    }

    private Map<String, Object> buildDeviceTestQueryParams(String deviceToken) {
        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("limit", 1);
        if (StringUtils.isNotBlank(deviceToken)) {
            queryParams.put("device_token", deviceToken.trim());
        }
        return queryParams;
    }

    private String normalizeTargetPath(String rawPath) {
        if (StringUtils.isBlank(rawPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "path is required");
        }
        String path = rawPath.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            path = URI.create(path).getPath();
        }
        if (path.startsWith("/api/")) {
            path = path.substring(4);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    private String normalizeDatasetLookupPath(String rawPath) {
        String path = SensorRouteAliasSupport.canonicalizeApiPath(rawPath);
        if (StringUtils.isBlank(path)) {
            path = normalizeTargetPath(rawPath);
        }
        if (path.startsWith("/api/")) {
            path = path.substring(4);
        }
        if (path.startsWith("/sensor/")) {
            return path.substring("/sensor".length());
        }
        return path;
    }

    private String normalizeManagedDataEndpoint(String rawPath) {
        if (StringUtils.isBlank(rawPath)) {
            return null;
        }
        return StringUtils.trimToNull(SensorRouteAliasSupport.canonicalizeApiPath(rawPath));
    }

    private Map<String, Object> toRealtimeChannelMap(SensorRealtimeChannel channel) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channelCode", channel.getChannelCode());
        result.put("name", channel.getName());
        result.put("wsPath", buildPlatformRealtimeWsUrl());
        result.put("topic", channel.getTopic());
        result.put("deviceType", channel.getDeviceType());
        result.put("authType", channel.getAuthType());
        result.put("status", channel.getStatus());
        result.put("updateTime", channel.getUpdateTime());
        return result;
    }

    private String buildPlatformRealtimeWsUrl() {
        try {
            URI uri = URI.create(StringUtils.defaultIfBlank(platformPublicBaseUrl, "http://localhost:7529/api"));
            String scheme = "https".equalsIgnoreCase(uri.getScheme()) ? "wss" : "ws";
            String host = StringUtils.defaultIfBlank(uri.getHost(), "localhost");
            int port = uri.getPort();
            StringBuilder builder = new StringBuilder();
            builder.append(scheme).append("://").append(host);
            if (port > 0) {
                builder.append(":").append(port);
            }
            String path = StringUtils.defaultIfBlank(uri.getPath(), "");
            if (StringUtils.isNotBlank(path)) {
                builder.append(StringUtils.removeEnd(path, "/"));
            }
            builder.append("/ws/sensor/realtime");
            return builder.toString();
        } catch (Exception e) {
            return "ws://localhost:7529/api/ws/sensor/realtime";
        }
    }

    private List<Map<String, Object>> buildFallbackRealtimeChannels(List<Map<String, Object>> devices) {
        String wsUrl = resolveRealtimeWsUrl();
        return devices.stream()
                .filter(device -> StringUtils.isNotBlank(getStringValue(device, "realtime_key")))
                .map(device -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    String realtimeKey = getStringValue(device, "realtime_key");
                    result.put("channelCode", "SENSOR_WS_" + realtimeKey);
                    result.put("name", getStringValue(device, "name") + " Realtime");
                    result.put("wsPath", wsUrl);
                    result.put("topic", realtimeKey);
                    result.put("deviceType", getStringValue(device, "type"));
                    result.put("authType", "session");
                    result.put("status", toBooleanStatus(device.get("enabled")));
                    return result;
                })
                .collect(Collectors.toList());
    }

    private Integer toBooleanStatus(Object value) {
        return toBooleanValue(value) ? 1 : 0;
    }

    private SensorDevice saveManagedDevice(Map<String, Object> devicePayload, SensorDevice existingDevice) {
        String deviceCode = StringUtils.trimToNull(firstNonBlank(
                getStringValue(devicePayload, "deviceCode"),
                getStringValue(devicePayload, "id")
        ));
        String deviceName = StringUtils.trimToNull(firstNonBlank(
                getStringValue(devicePayload, "deviceName"),
                getStringValue(devicePayload, "name")
        ));
        String deviceType = StringUtils.trimToNull(firstNonBlank(
                getStringValue(devicePayload, "deviceType"),
                getStringValue(devicePayload, "type")
        ));
        if (StringUtils.isAnyBlank(deviceCode, deviceName, deviceType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "device id, name and type are required");
        }
        QueryWrapper<SensorDevice> duplicateQueryWrapper = new QueryWrapper<SensorDevice>().eq("deviceCode", deviceCode);
        if (existingDevice != null && existingDevice.getId() != null) {
            duplicateQueryWrapper.ne("id", existingDevice.getId());
        }
        if (sensorDeviceService.count(duplicateQueryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "deviceCode already exists");
        }
        SensorDataSource dataSource = getOrCreateManagedDataSource();
        String oldRealtimeKey = existingDevice == null ? null : existingDevice.getRealtimeKey();
        SensorDevice sensorDevice = existingDevice == null ? new SensorDevice() : existingDevice;
        sensorDevice.setDeviceCode(deviceCode);
        sensorDevice.setDeviceName(deviceName);
        sensorDevice.setDeviceType(deviceType);
        sensorDevice.setCategory(StringUtils.trimToNull(getStringValue(devicePayload, "category")));
        sensorDevice.setDescription(StringUtils.trimToNull(getStringValue(devicePayload, "description")));
        sensorDevice.setDeviceToken(StringUtils.trimToNull(firstNonBlank(
                getStringValue(devicePayload, "deviceToken"),
                getStringValue(devicePayload, "token")
        )));
        sensorDevice.setDataSourceId(dataSource.getId());
        sensorDevice.setLegacyDeviceId(StringUtils.defaultIfBlank(
                StringUtils.trimToNull(getStringValue(devicePayload, "legacyDeviceId")),
                deviceCode
        ));
        sensorDevice.setDataEndpoint(normalizeManagedDataEndpoint(firstNonBlank(
                getStringValue(devicePayload, "dataEndpoint"),
                getStringValue(devicePayload, "data_endpoint")
        )));
        sensorDevice.setRealtimeKey(StringUtils.trimToNull(firstNonBlank(
                getStringValue(devicePayload, "realtimeKey"),
                getStringValue(devicePayload, "realtime_key")
        )));
        sensorDevice.setConfigJson(buildManagedConfigJson(devicePayload, sensorDevice.getConfigJson()));
        sensorDevice.setDataFieldsJson(buildJsonString(
                firstNonNull(devicePayload.get("data_fields"), devicePayload.get("dataFields"), devicePayload.get("dataFieldsJson"))
        ));
        sensorDevice.setStatus(resolveManagedStatus(devicePayload));
        boolean result = existingDevice == null ? sensorDeviceService.save(sensorDevice) : sensorDeviceService.updateById(sensorDevice);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "failed to save managed device");
        }
        syncManagedRealtimeChannel(oldRealtimeKey, sensorDevice, dataSource);
        return sensorDevice;
    }

    private SensorDevice findManagedDevice(String deviceId) {
        String normalizedId = deviceId.trim();
        SensorDevice sensorDevice = sensorDeviceService.getOne(
                new QueryWrapper<SensorDevice>().eq("deviceCode", normalizedId),
                false
        );
        if (sensorDevice == null) {
            sensorDevice = sensorDeviceService.getOne(
                    new QueryWrapper<SensorDevice>().lambda().eq(SensorDevice::getLegacyDeviceId, normalizedId),
                    false
            );
        }
        if (sensorDevice == null && StringUtils.isNumeric(normalizedId)) {
            sensorDevice = sensorDeviceService.getById(Long.parseLong(normalizedId));
        }
        if (sensorDevice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "managed sensor device not found");
        }
        return sensorDevice;
    }

    private SensorDataSource getOrCreateManagedDataSource() {
        SensorDataSource dataSource = sensorDataSourceService.getOne(
                new QueryWrapper<SensorDataSource>().eq("code", DEFAULT_SOURCE_CODE),
                false
        );
        if (dataSource != null) {
            boolean changed = false;
            if (!StringUtils.equals(dataSource.getCode(), DEFAULT_SOURCE_CODE)) {
                dataSource.setCode(DEFAULT_SOURCE_CODE);
                changed = true;
            }
            if (!StringUtils.equals(dataSource.getName(), DEFAULT_SOURCE_NAME)) {
                dataSource.setName(DEFAULT_SOURCE_NAME);
                changed = true;
            }
            if (!StringUtils.equals(dataSource.getBaseUrl(), platformPublicBaseUrl)) {
                dataSource.setBaseUrl(platformPublicBaseUrl);
                changed = true;
            }
            if (!StringUtils.equals(dataSource.getWsUrl(), resolveRealtimeWsUrl())) {
                dataSource.setWsUrl(resolveRealtimeWsUrl());
                changed = true;
            }
            if (changed) {
                sensorDataSourceService.updateById(dataSource);
            }
            return dataSource;
        }
        dataSource = new SensorDataSource();
        dataSource.setCode(DEFAULT_SOURCE_CODE);
        dataSource.setName(DEFAULT_SOURCE_NAME);
        dataSource.setType("mixed");
        dataSource.setBaseUrl(platformPublicBaseUrl);
        dataSource.setWsUrl(resolveRealtimeWsUrl());
        dataSource.setStatus(1);
        dataSource.setConfigJson("{\"managedBy\":\"SensorHub\",\"phase\":\"phase4-brand-cleanup\"}");
        boolean saved = sensorDataSourceService.save(dataSource);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "failed to initialize sensor data source");
        }
        return dataSource;
    }

    private void syncManagedRealtimeChannel(String oldRealtimeKey,
                                            SensorDevice sensorDevice,
                                            SensorDataSource dataSource) {
        if (StringUtils.isNotBlank(oldRealtimeKey)
                && !StringUtils.equals(oldRealtimeKey, sensorDevice.getRealtimeKey())) {
            sensorRealtimeChannelService.remove(
                    new QueryWrapper<SensorRealtimeChannel>()
                            .eq("channelCode", buildManagedRealtimeChannelCode(oldRealtimeKey))
            );
        }
        if (StringUtils.isBlank(sensorDevice.getRealtimeKey())) {
            return;
        }
        String channelCode = buildManagedRealtimeChannelCode(sensorDevice.getRealtimeKey());
        SensorRealtimeChannel sensorRealtimeChannel = sensorRealtimeChannelService.getOne(
                new QueryWrapper<SensorRealtimeChannel>().eq("channelCode", channelCode),
                false
        );
        boolean isNew = sensorRealtimeChannel == null;
        if (isNew) {
            sensorRealtimeChannel = new SensorRealtimeChannel();
            sensorRealtimeChannel.setChannelCode(channelCode);
        }
        sensorRealtimeChannel.setName(sensorDevice.getDeviceName() + " Realtime");
        sensorRealtimeChannel.setWsPath(StringUtils.defaultIfBlank(dataSource.getWsUrl(), resolveRealtimeWsUrl()));
        sensorRealtimeChannel.setTopic(sensorDevice.getRealtimeKey());
        sensorRealtimeChannel.setDeviceType(sensorDevice.getDeviceType());
        sensorRealtimeChannel.setAuthType("session");
        sensorRealtimeChannel.setStatus(sensorDevice.getStatus() == null ? 1 : sensorDevice.getStatus());
        if (isNew) {
            sensorRealtimeChannelService.save(sensorRealtimeChannel);
        } else {
            sensorRealtimeChannelService.updateById(sensorRealtimeChannel);
        }
    }

    private Map<String, Object> toManagedDeviceMap(SensorDevice sensorDevice) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> configMap = parseJsonObject(sensorDevice.getConfigJson());
        List<Map<String, Object>> dataFields = parseJsonList(sensorDevice.getDataFieldsJson());
        String canonicalDataEndpoint = normalizeManagedDataEndpoint(sensorDevice.getDataEndpoint());
        result.put("id", sensorDevice.getDeviceCode());
        result.put("deviceCode", sensorDevice.getDeviceCode());
        result.put("name", sensorDevice.getDeviceName());
        result.put("deviceName", sensorDevice.getDeviceName());
        result.put("type", sensorDevice.getDeviceType());
        result.put("deviceType", sensorDevice.getDeviceType());
        result.put("category", sensorDevice.getCategory());
        result.put("description", sensorDevice.getDescription());
        result.put("token", sensorDevice.getDeviceToken());
        result.put("deviceToken", sensorDevice.getDeviceToken());
        result.put("dataSourceId", sensorDevice.getDataSourceId());
        result.put("legacyDeviceId", sensorDevice.getLegacyDeviceId());
        result.put("data_endpoint", canonicalDataEndpoint);
        result.put("dataEndpoint", canonicalDataEndpoint);
        result.put("realtime_key", sensorDevice.getRealtimeKey());
        result.put("realtimeKey", sensorDevice.getRealtimeKey());
        result.put("status", sensorDevice.getStatus());
        result.put("enabled", sensorDevice.getStatus() != null && sensorDevice.getStatus() == 1);
        result.put("config", configMap);
        result.put("data_fields", dataFields);
        result.put("icon", configMap.get("icon"));
        result.put("color", configMap.get("color"));
        result.put("created_at", sensorDevice.getCreateTime());
        result.put("updated_at", sensorDevice.getUpdateTime());
        return result;
    }

    private Map<String, Object> parseJsonObject(String jsonText) {
        if (StringUtils.isBlank(jsonText)) {
            return new LinkedHashMap<>();
        }
        Type mapType = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> result = gson.fromJson(jsonText, mapType);
        return result == null ? new LinkedHashMap<>() : new LinkedHashMap<>(result);
    }

    private List<Map<String, Object>> parseJsonList(String jsonText) {
        if (StringUtils.isBlank(jsonText)) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<Map<String, Object>>>() {
        }.getType();
        List<Map<String, Object>> result = gson.fromJson(jsonText, listType);
        return result == null ? Collections.emptyList() : result;
    }

    private String buildManagedConfigJson(Map<String, Object> devicePayload, String existingConfigJson) {
        Map<String, Object> configMap = parseJsonObject(existingConfigJson);
        Object incomingConfig = firstNonNull(devicePayload.get("config"), devicePayload.get("configJson"));
        if (incomingConfig instanceof Map) {
            configMap.putAll((Map<String, Object>) incomingConfig);
        } else if (incomingConfig instanceof String && StringUtils.isNotBlank((String) incomingConfig)) {
            configMap.putAll(parseJsonObject((String) incomingConfig));
        }
        String icon = StringUtils.trimToNull(getStringValue(devicePayload, "icon"));
        String color = StringUtils.trimToNull(getStringValue(devicePayload, "color"));
        if (icon != null) {
            configMap.put("icon", icon);
        }
        if (color != null) {
            configMap.put("color", color);
        }
        configMap.put("managedBy", "SensorHub");
        configMap.put("phase", "phase3");
        return gson.toJson(configMap);
    }

    private String buildJsonString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return StringUtils.trimToNull((String) value);
        }
        return gson.toJson(value);
    }

    private Integer resolveManagedStatus(Map<String, Object> devicePayload) {
        Object statusValue = firstNonNull(devicePayload.get("status"), devicePayload.get("enabled"));
        if (statusValue == null) {
            return 1;
        }
        if (statusValue instanceof Number) {
            return ((Number) statusValue).intValue() > 0 ? 1 : 0;
        }
        return toBooleanValue(statusValue) ? 1 : 0;
    }

    private String buildManagedRealtimeChannelCode(String realtimeKey) {
        String raw = "SENSOR_WS_" + realtimeKey;
        return raw.replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "").toUpperCase();
    }

    private Object firstNonNull(Object first, Object second, Object third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.isNotBlank(first) ? first : second;
    }

    private boolean toBooleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() > 0;
        }
        return StringUtils.equalsAnyIgnoreCase(String.valueOf(value), "true", "1");
    }

    private static class SensorDatasetDefinition {
        private final String code;
        private final String name;
        private final String gatewayPath;
        private final String targetPath;
        private final String statsKey;

        private SensorDatasetDefinition(String code, String name, String gatewayPath, String targetPath, String statsKey) {
            this.code = code;
            this.name = name;
            this.gatewayPath = gatewayPath;
            this.targetPath = targetPath;
            this.statsKey = statsKey;
        }

        private String getCode() {
            return code;
        }

        private String getName() {
            return name;
        }

        private String getGatewayPath() {
            return gatewayPath;
        }

        private String getTargetPath() {
            return targetPath;
        }

        private String getStatsKey() {
            return statsKey;
        }
    }
}
