package com.sensorhub.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.sensorhub.platform.common.ErrorCode;
import com.sensorhub.platform.exception.BusinessException;
import com.sensorhub.platform.model.dto.sensor.SensorApiEndpointQueryRequest;
import com.sensorhub.platform.model.dto.sensor.SensorDeviceQueryRequest;
import com.sensorhub.platform.model.dto.sensor.SensorInterfaceBindingQueryRequest;
import com.sensorhub.platform.model.dto.sensor.SensorRealtimeChannelQueryRequest;
import com.sensorhub.platform.model.entity.SensorApiEndpoint;
import com.sensorhub.platform.model.entity.SensorDataSource;
import com.sensorhub.platform.model.entity.SensorDevice;
import com.sensorhub.platform.model.entity.SensorInterfaceBinding;
import com.sensorhub.platform.model.entity.SensorRealtimeChannel;
import com.sensorhub.platform.model.enums.InterfaceInfoStatusEnum;
import com.sensorhub.platform.model.vo.SensorInterfaceBindingVO;
import com.sensorhub.platform.model.vo.SensorRegistryBootstrapResultVO;
import com.sensorhub.platform.model.vo.SensorRegistryDashboardVO;
import com.sensorhub.platform.model.vo.SensorRegistrySyncResultVO;
import com.sensorhub.platform.model.vo.SensorWorkspaceCatalogSyncResultVO;
import com.sensorhub.platform.model.vo.SensorWorkspaceInterfaceVO;
import com.sensorhub.platform.service.SensorApiEndpointService;
import com.sensorhub.platform.service.SensorDataSourceService;
import com.sensorhub.platform.service.SensorDeviceService;
import com.sensorhub.platform.service.SensorInterfaceBindingService;
import com.sensorhub.platform.service.SensorRealtimeChannelService;
import com.sensorhub.platform.service.SensorRegistryService;
import com.sensorhub.platform.service.SensorWorkspaceService;
import com.sensorhub.common.model.entity.InterfaceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SensorRegistryServiceImpl implements SensorRegistryService {

    private static final String DEFAULT_SOURCE_CODE = "API_EXAMPLE_LOCAL";
    private static final String DEFAULT_SOURCE_NAME = "SensorHub Local Data Source";
    private static final String DEFAULT_SOURCE_TYPE = "mixed";
    private static final String DEFAULT_TARGET_SERVICE = "sensorhub-backend";
    private static final String DEFAULT_PROTOCOL = "http";
    private static final String DEFAULT_PUBLISH_STRATEGY = "proxy";
    private static final String DEFAULT_AUTH_STRATEGY = "gateway_aksk";
    private static final String DEFAULT_REALTIME_AUTH_TYPE = "session";
    private static final String ENDPOINT_CODE_PREFIX = "SENSOR";
    private static final String CHANNEL_CODE_PREFIX = "SENSOR_WS";

    @Resource
    private SensorDataSourceService sensorDataSourceService;

    @Resource
    private SensorDeviceService sensorDeviceService;

    @Resource
    private SensorApiEndpointService sensorApiEndpointService;

    @Resource
    private SensorInterfaceBindingService sensorInterfaceBindingService;

    @Resource
    private SensorRealtimeChannelService sensorRealtimeChannelService;

    @Resource
    private SensorWorkspaceService sensorWorkspaceService;

    @Resource
    private com.sensorhub.platform.service.InterfaceInfoService interfaceInfoService;

    @Value("${platform.public-base-url:http://localhost:7529/api}")
    private String platformPublicBaseUrl;

    private final Gson gson = new Gson();

    @Override
    public SensorRegistryDashboardVO getDashboard() {
        SensorDataSource defaultDataSource = getOrCreateDefaultDataSource();
        SensorRegistryDashboardVO dashboardVO = new SensorRegistryDashboardVO();
        dashboardVO.setDataSourceCount(sensorDataSourceService.count());
        dashboardVO.setDeviceCount(sensorDeviceService.count());
        dashboardVO.setEndpointCount(sensorApiEndpointService.count());
        dashboardVO.setBindingCount(sensorInterfaceBindingService.count());
        dashboardVO.setRealtimeChannelCount(sensorRealtimeChannelService.count());
        dashboardVO.setPlatformInterfaceCount(interfaceInfoService.count());
        QueryWrapper<InterfaceInfo> interfaceQueryWrapper = new QueryWrapper<>();
        interfaceQueryWrapper.eq("status", InterfaceInfoStatusEnum.ONLINE.getValue());
        dashboardVO.setPublishedInterfaceCount(interfaceInfoService.count(interfaceQueryWrapper));
        dashboardVO.setDefaultDataSourceCode(defaultDataSource.getCode());
        return dashboardVO;
    }

    @Override
    public SensorRegistryBootstrapResultVO bootstrapRegistry() {
        SensorDataSource defaultSource = getOrCreateDefaultDataSource();
        SensorWorkspaceCatalogSyncResultVO interfaceCatalogSync = sensorWorkspaceService.syncNorthboundInterfaces();
        SensorRegistrySyncResultVO deviceSync = syncDevicesFromWorkspace();
        SensorRegistrySyncResultVO endpointSync = syncApiEndpointsFromWorkspace();
        SensorRegistrySyncResultVO bindingSync = syncInterfaceBindings();
        SensorRegistrySyncResultVO realtimeChannelSync = syncRealtimeChannels();
        SensorRegistryBootstrapResultVO resultVO = new SensorRegistryBootstrapResultVO();
        resultVO.setDataSourceCode(defaultSource.getCode());
        resultVO.setInterfaceCatalogSync(interfaceCatalogSync);
        resultVO.setDeviceSync(deviceSync);
        resultVO.setEndpointSync(endpointSync);
        resultVO.setBindingSync(bindingSync);
        resultVO.setRealtimeChannelSync(realtimeChannelSync);
        return resultVO;
    }

    @Override
    public SensorRegistrySyncResultVO syncDevicesFromWorkspace() {
        SensorDataSource defaultSource = getOrCreateDefaultDataSource();
        List<Map<String, Object>> workspaceDevices = sensorWorkspaceService.listSensorDevices();
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        for (Map<String, Object> workspaceDevice : workspaceDevices) {
            String deviceCode = getStringValue(workspaceDevice, "id");
            if (StringUtils.isBlank(deviceCode)) {
                continue;
            }
            SensorDevice sensorDevice = sensorDeviceService.getOne(
                    new QueryWrapper<SensorDevice>().eq("deviceCode", deviceCode),
                    false
            );
            boolean isNew = sensorDevice == null;
            if (isNew) {
                sensorDevice = new SensorDevice();
                sensorDevice.setDeviceCode(deviceCode);
            }
            sensorDevice.setDeviceName(getStringValue(workspaceDevice, "name"));
            sensorDevice.setDeviceType(getStringValue(workspaceDevice, "type"));
            sensorDevice.setCategory(getStringValue(workspaceDevice, "category"));
            sensorDevice.setDescription(getStringValue(workspaceDevice, "description"));
            sensorDevice.setDeviceToken(getStringValue(workspaceDevice, "token"));
            sensorDevice.setDataSourceId(defaultSource.getId());
            sensorDevice.setLegacyDeviceId(deviceCode);
            sensorDevice.setDataEndpoint(getStringValue(workspaceDevice, "data_endpoint"));
            sensorDevice.setRealtimeKey(getStringValue(workspaceDevice, "realtime_key"));
            sensorDevice.setConfigJson(toJsonString(workspaceDevice.get("config")));
            sensorDevice.setDataFieldsJson(toJsonString(workspaceDevice.get("data_fields")));
            sensorDevice.setStatus(toBooleanValue(workspaceDevice.get("enabled")) ? 1 : 0);
            if (isNew) {
                sensorDeviceService.save(sensorDevice);
                created.add(deviceCode);
            } else {
                sensorDeviceService.updateById(sensorDevice);
                updated.add(deviceCode);
            }
        }
        return buildSyncResult("devices", workspaceDevices.size(), created, updated, Collections.emptyList());
    }

    @Override
    public SensorRegistrySyncResultVO syncApiEndpointsFromWorkspace() {
        Map<String, SensorWorkspaceInterfaceVO> workspaceInterfaceMap = getWorkspaceInterfaceMapByTargetPath();
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        Set<String> processedKeys = new java.util.LinkedHashSet<>();
        syncNorthboundOnlyEndpoints(workspaceInterfaceMap, processedKeys, created, updated);
        return buildSyncResult("apiEndpoints", workspaceInterfaceMap.size(), created, updated, Collections.emptyList());
    }

    @Override
    public SensorRegistrySyncResultVO syncInterfaceBindings() {
        List<SensorApiEndpoint> endpointList = sensorApiEndpointService.list();
        if (endpointList.isEmpty()) {
            return buildSyncResult("bindings", 0, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        Map<String, SensorWorkspaceInterfaceVO> workspaceInterfaceMap = getWorkspaceInterfaceMapByTargetPath();
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (SensorApiEndpoint endpoint : endpointList) {
            SensorWorkspaceInterfaceVO northboundInterface = workspaceInterfaceMap.get(
                    buildMethodPathKey(endpoint.getMethod(), endpoint.getTargetPath())
            );
            if (northboundInterface == null || northboundInterface.getInterfaceInfoId() == null) {
                skipped.add(endpoint.getEndpointCode());
                continue;
            }
            SensorInterfaceBinding binding = sensorInterfaceBindingService.getOne(
                    new QueryWrapper<SensorInterfaceBinding>()
                            .eq("sensorApiEndpointId", endpoint.getId())
                            .eq("interfaceInfoId", northboundInterface.getInterfaceInfoId()),
                    false
            );
            boolean isNew = binding == null;
            if (isNew) {
                binding = new SensorInterfaceBinding();
                binding.setSensorApiEndpointId(endpoint.getId());
                binding.setInterfaceInfoId(northboundInterface.getInterfaceInfoId());
                binding.setPublishStrategy(DEFAULT_PUBLISH_STRATEGY);
                binding.setAuthStrategy(DEFAULT_AUTH_STRATEGY);
            }
            binding.setStatus(endpoint.getStatus());
            if (isNew) {
                sensorInterfaceBindingService.save(binding);
                created.add(endpoint.getEndpointCode());
            } else {
                sensorInterfaceBindingService.updateById(binding);
                updated.add(endpoint.getEndpointCode());
            }
        }
        return buildSyncResult("bindings", endpointList.size(), created, updated, skipped);
    }

    @Override
    public SensorRegistrySyncResultVO syncRealtimeChannels() {
        SensorDataSource defaultSource = getOrCreateDefaultDataSource();
        List<SensorDevice> deviceList = sensorDeviceService.list(
                new QueryWrapper<SensorDevice>().orderByDesc("updateTime")
        );
        if (deviceList.isEmpty()) {
            syncDevicesFromWorkspace();
            deviceList = sensorDeviceService.list(new QueryWrapper<SensorDevice>().orderByDesc("updateTime"));
        }
        Map<String, SensorDevice> realtimeDeviceMap = deviceList.stream()
                .filter(device -> StringUtils.isNotBlank(device.getRealtimeKey()))
                .sorted(Comparator.comparing(SensorDevice::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toMap(
                        SensorDevice::getRealtimeKey,
                        device -> device,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
        Map<String, SensorRealtimeChannel> existingChannelMap = sensorRealtimeChannelService.list().stream()
                .collect(Collectors.toMap(SensorRealtimeChannel::getChannelCode, item -> item, (left, right) -> left));
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (SensorDevice device : deviceList) {
            if (StringUtils.isBlank(device.getRealtimeKey())) {
                skipped.add(device.getDeviceCode());
            }
        }
        for (Map.Entry<String, SensorDevice> entry : realtimeDeviceMap.entrySet()) {
            String realtimeKey = entry.getKey();
            SensorDevice device = entry.getValue();
            String channelCode = buildRealtimeChannelCode(realtimeKey);
            SensorRealtimeChannel channel = existingChannelMap.get(channelCode);
            boolean isNew = channel == null;
            if (isNew) {
                channel = new SensorRealtimeChannel();
            }
            channel.setChannelCode(channelCode);
            channel.setName(buildRealtimeChannelName(device));
            channel.setWsPath(StringUtils.defaultIfBlank(defaultSource.getWsUrl(), buildDefaultWsUrl()));
            channel.setTopic(realtimeKey);
            channel.setDeviceType(device.getDeviceType());
            channel.setAuthType(DEFAULT_REALTIME_AUTH_TYPE);
            channel.setStatus(device.getStatus() == null ? 1 : device.getStatus());
            if (isNew) {
                sensorRealtimeChannelService.save(channel);
                created.add(channelCode);
            } else {
                sensorRealtimeChannelService.updateById(channel);
                updated.add(channelCode);
            }
        }
        return buildSyncResult("realtimeChannels", realtimeDeviceMap.size(), created, updated, skipped);
    }

    @Override
    public List<SensorDataSource> listDataSources() {
        QueryWrapper<SensorDataSource> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("updateTime");
        return sensorDataSourceService.list(queryWrapper);
    }

    @Override
    public List<SensorDevice> listDevices(SensorDeviceQueryRequest queryRequest) {
        return sensorDeviceService.list(buildSensorDeviceQueryWrapper(queryRequest));
    }

    @Override
    public Page<SensorDevice> listDevicesByPage(SensorDeviceQueryRequest queryRequest) {
        long current = queryRequest == null ? 1 : queryRequest.getCurrent();
        long pageSize = queryRequest == null ? 10 : Math.min(queryRequest.getPageSize(), 100);
        return sensorDeviceService.page(new Page<>(current, pageSize), buildSensorDeviceQueryWrapper(queryRequest));
    }

    @Override
    public Page<SensorApiEndpoint> listApiEndpointsByPage(SensorApiEndpointQueryRequest queryRequest) {
        SensorApiEndpoint safeQuery = new SensorApiEndpoint();
        if (queryRequest != null) {
            BeanUtils.copyProperties(queryRequest, safeQuery);
        }
        long current = queryRequest == null ? 1 : queryRequest.getCurrent();
        long pageSize = queryRequest == null ? 10 : Math.min(queryRequest.getPageSize(), 100);
        QueryWrapper<SensorApiEndpoint> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(safeQuery.getEndpointCode()), "endpointCode", safeQuery.getEndpointCode());
        queryWrapper.like(StringUtils.isNotBlank(safeQuery.getName()), "name", safeQuery.getName());
        queryWrapper.eq(StringUtils.isNotBlank(safeQuery.getMethod()), "method", safeQuery.getMethod());
        queryWrapper.eq(StringUtils.isNotBlank(safeQuery.getTargetService()), "targetService", safeQuery.getTargetService());
        queryWrapper.eq(StringUtils.isNotBlank(safeQuery.getCategory()), "category", safeQuery.getCategory());
        queryWrapper.eq(safeQuery.getStatus() != null, "status", safeQuery.getStatus());
        queryWrapper.orderByDesc("updateTime");
        return sensorApiEndpointService.page(new Page<>(current, pageSize), queryWrapper);
    }

    @Override
    public Page<SensorInterfaceBindingVO> listBindingsByPage(SensorInterfaceBindingQueryRequest queryRequest) {
        long current = queryRequest == null ? 1 : queryRequest.getCurrent();
        long pageSize = queryRequest == null ? 10 : Math.min(queryRequest.getPageSize(), 100);
        QueryWrapper<SensorInterfaceBinding> queryWrapper = new QueryWrapper<>();
        if (queryRequest != null) {
            queryWrapper.eq(queryRequest.getSensorApiEndpointId() != null,
                    "sensorApiEndpointId",
                    queryRequest.getSensorApiEndpointId());
            queryWrapper.eq(queryRequest.getInterfaceInfoId() != null,
                    "interfaceInfoId",
                    queryRequest.getInterfaceInfoId());
            queryWrapper.eq(queryRequest.getStatus() != null, "status", queryRequest.getStatus());
        }
        queryWrapper.orderByDesc("updateTime");
        Page<SensorInterfaceBinding> bindingPage = sensorInterfaceBindingService.page(new Page<>(current, pageSize), queryWrapper);
        List<SensorInterfaceBindingVO> voList = toBindingVOList(bindingPage.getRecords());
        Page<SensorInterfaceBindingVO> resultPage = new Page<>(bindingPage.getCurrent(), bindingPage.getSize(), bindingPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public Page<SensorRealtimeChannel> listRealtimeChannelsByPage(SensorRealtimeChannelQueryRequest queryRequest) {
        SensorRealtimeChannel safeQuery = new SensorRealtimeChannel();
        if (queryRequest != null) {
            BeanUtils.copyProperties(queryRequest, safeQuery);
        }
        long current = queryRequest == null ? 1 : queryRequest.getCurrent();
        long pageSize = queryRequest == null ? 10 : Math.min(queryRequest.getPageSize(), 100);
        QueryWrapper<SensorRealtimeChannel> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(safeQuery.getChannelCode()), "channelCode", safeQuery.getChannelCode());
        queryWrapper.like(StringUtils.isNotBlank(safeQuery.getName()), "name", safeQuery.getName());
        queryWrapper.eq(StringUtils.isNotBlank(safeQuery.getDeviceType()), "deviceType", safeQuery.getDeviceType());
        queryWrapper.eq(StringUtils.isNotBlank(safeQuery.getAuthType()), "authType", safeQuery.getAuthType());
        queryWrapper.eq(safeQuery.getStatus() != null, "status", safeQuery.getStatus());
        queryWrapper.orderByDesc("updateTime");
        return sensorRealtimeChannelService.page(new Page<>(current, pageSize), queryWrapper);
    }

    @Override
    public Long addSensorDevice(SensorDevice sensorDevice) {
        SensorDevice savedDevice = saveSensorDeviceInternal(sensorDevice, true);
        return savedDevice.getId();
    }

    @Override
    public boolean updateSensorDevice(SensorDevice sensorDevice) {
        saveSensorDeviceInternal(sensorDevice, false);
        return true;
    }

    @Override
    public boolean deleteSensorDevice(Long id) {
        SensorDevice oldDevice = sensorDeviceService.getById(id);
        if (oldDevice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "sensor device not found");
        }
        boolean removed = sensorDeviceService.removeById(id);
        if (removed && StringUtils.isNotBlank(oldDevice.getRealtimeKey())) {
            removeRealtimeChannelByKey(oldDevice.getRealtimeKey());
        }
        return removed;
    }

    @Override
    public Long addSensorApiEndpoint(SensorApiEndpoint sensorApiEndpoint) {
        SensorApiEndpoint savedEndpoint = saveSensorApiEndpointInternal(sensorApiEndpoint, true);
        return savedEndpoint.getId();
    }

    @Override
    public boolean updateSensorApiEndpoint(SensorApiEndpoint sensorApiEndpoint) {
        saveSensorApiEndpointInternal(sensorApiEndpoint, false);
        return true;
    }

    @Override
    public boolean deleteSensorApiEndpoint(Long id) {
        SensorApiEndpoint oldEndpoint = sensorApiEndpointService.getById(id);
        if (oldEndpoint == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "sensor api endpoint not found");
        }
        sensorInterfaceBindingService.remove(
                new QueryWrapper<SensorInterfaceBinding>().eq("sensorApiEndpointId", id)
        );
        return sensorApiEndpointService.removeById(id);
    }

    @Override
    public Long addSensorInterfaceBinding(SensorInterfaceBinding sensorInterfaceBinding) {
        SensorInterfaceBinding savedBinding = saveSensorInterfaceBindingInternal(sensorInterfaceBinding, true);
        return savedBinding.getId();
    }

    @Override
    public boolean updateSensorInterfaceBinding(SensorInterfaceBinding sensorInterfaceBinding) {
        saveSensorInterfaceBindingInternal(sensorInterfaceBinding, false);
        return true;
    }

    @Override
    public boolean deleteSensorInterfaceBinding(Long id) {
        SensorInterfaceBinding oldBinding = sensorInterfaceBindingService.getById(id);
        if (oldBinding == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "sensor interface binding not found");
        }
        return sensorInterfaceBindingService.removeById(id);
    }

    @Override
    public Long addSensorRealtimeChannel(SensorRealtimeChannel sensorRealtimeChannel) {
        SensorRealtimeChannel savedChannel = saveSensorRealtimeChannelInternal(sensorRealtimeChannel, true);
        return savedChannel.getId();
    }

    @Override
    public boolean updateSensorRealtimeChannel(SensorRealtimeChannel sensorRealtimeChannel) {
        saveSensorRealtimeChannelInternal(sensorRealtimeChannel, false);
        return true;
    }

    @Override
    public boolean deleteSensorRealtimeChannel(Long id) {
        SensorRealtimeChannel oldChannel = sensorRealtimeChannelService.getById(id);
        if (oldChannel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "sensor realtime channel not found");
        }
        return sensorRealtimeChannelService.removeById(id);
    }

    private List<SensorInterfaceBindingVO> toBindingVOList(List<SensorInterfaceBinding> bindingList) {
        if (bindingList == null || bindingList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> endpointIds = bindingList.stream().map(SensorInterfaceBinding::getSensorApiEndpointId).collect(Collectors.toList());
        List<Long> interfaceIds = bindingList.stream().map(SensorInterfaceBinding::getInterfaceInfoId).collect(Collectors.toList());
        Map<Long, SensorApiEndpoint> endpointMap = sensorApiEndpointService.listByIds(endpointIds).stream()
                .collect(Collectors.toMap(SensorApiEndpoint::getId, item -> item));
        Map<Long, InterfaceInfo> interfaceInfoMap = interfaceInfoService.listByIds(interfaceIds).stream()
                .collect(Collectors.toMap(InterfaceInfo::getId, item -> item));
        List<SensorInterfaceBindingVO> resultList = new ArrayList<>();
        for (SensorInterfaceBinding binding : bindingList) {
            SensorInterfaceBindingVO vo = new SensorInterfaceBindingVO();
            BeanUtils.copyProperties(binding, vo);
            SensorApiEndpoint endpoint = endpointMap.get(binding.getSensorApiEndpointId());
            if (endpoint != null) {
                vo.setEndpointCode(endpoint.getEndpointCode());
                vo.setEndpointName(endpoint.getName());
                vo.setInternalPath(endpoint.getInternalPath());
                vo.setMethod(endpoint.getMethod());
            }
            InterfaceInfo interfaceInfo = interfaceInfoMap.get(binding.getInterfaceInfoId());
            if (interfaceInfo != null) {
                vo.setInterfaceName(interfaceInfo.getName());
                vo.setInterfaceUrl(interfaceInfo.getUrl());
                vo.setInterfaceStatus(interfaceInfo.getStatus());
            }
            resultList.add(vo);
        }
        return resultList;
    }

    private SensorDevice saveSensorDeviceInternal(SensorDevice requestDevice, boolean isCreate) {
        if (requestDevice == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SensorDevice oldDevice = null;
        if (!isCreate) {
            if (requestDevice.getId() == null || requestDevice.getId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "device id is required");
            }
            oldDevice = sensorDeviceService.getById(requestDevice.getId());
            if (oldDevice == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "sensor device not found");
            }
        }
        SensorDevice sensorDevice = oldDevice == null ? new SensorDevice() : oldDevice;
        String deviceCode = StringUtils.trimToNull(requestDevice.getDeviceCode());
        String deviceName = StringUtils.trimToNull(requestDevice.getDeviceName());
        String deviceType = StringUtils.trimToNull(requestDevice.getDeviceType());
        if (StringUtils.isAnyBlank(deviceCode, deviceName, deviceType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "deviceCode, deviceName and deviceType are required");
        }
        QueryWrapper<SensorDevice> duplicateQueryWrapper = new QueryWrapper<SensorDevice>().eq("deviceCode", deviceCode);
        if (requestDevice.getId() != null) {
            duplicateQueryWrapper.ne("id", requestDevice.getId());
        }
        long duplicateCount = sensorDeviceService.count(duplicateQueryWrapper);
        if (duplicateCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "deviceCode already exists");
        }
        SensorDataSource defaultSource = getOrCreateDefaultDataSource();
        String oldRealtimeKey = oldDevice == null ? null : oldDevice.getRealtimeKey();
        sensorDevice.setDeviceCode(deviceCode);
        sensorDevice.setDeviceName(deviceName);
        sensorDevice.setDeviceType(deviceType);
        sensorDevice.setCategory(StringUtils.trimToNull(requestDevice.getCategory()));
        sensorDevice.setDescription(StringUtils.trimToNull(requestDevice.getDescription()));
        sensorDevice.setDeviceToken(StringUtils.trimToNull(requestDevice.getDeviceToken()));
        sensorDevice.setDataSourceId(requestDevice.getDataSourceId() == null ? defaultSource.getId() : requestDevice.getDataSourceId());
        sensorDevice.setLegacyDeviceId(StringUtils.defaultIfBlank(
                StringUtils.trimToNull(requestDevice.getLegacyDeviceId()),
                deviceCode
        ));
        sensorDevice.setDataEndpoint(StringUtils.trimToNull(requestDevice.getDataEndpoint()));
        sensorDevice.setRealtimeKey(StringUtils.trimToNull(requestDevice.getRealtimeKey()));
        sensorDevice.setConfigJson(trimNullableText(requestDevice.getConfigJson(), oldDevice == null ? null : oldDevice.getConfigJson()));
        sensorDevice.setDataFieldsJson(trimNullableText(requestDevice.getDataFieldsJson(), oldDevice == null ? null : oldDevice.getDataFieldsJson()));
        sensorDevice.setStatus(requestDevice.getStatus() == null ? 1 : requestDevice.getStatus());
        boolean result = isCreate ? sensorDeviceService.save(sensorDevice) : sensorDeviceService.updateById(sensorDevice);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "save sensor device failed");
        }
        syncDerivedRealtimeChannel(oldRealtimeKey, sensorDevice);
        return sensorDevice;
    }

    private SensorApiEndpoint saveSensorApiEndpointInternal(SensorApiEndpoint requestEndpoint, boolean isCreate) {
        if (requestEndpoint == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SensorApiEndpoint oldEndpoint = null;
        if (!isCreate) {
            if (requestEndpoint.getId() == null || requestEndpoint.getId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "endpoint id is required");
            }
            oldEndpoint = sensorApiEndpointService.getById(requestEndpoint.getId());
            if (oldEndpoint == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "sensor api endpoint not found");
            }
        }
        SensorApiEndpoint sensorApiEndpoint = oldEndpoint == null ? new SensorApiEndpoint() : oldEndpoint;
        String endpointCode = StringUtils.trimToNull(requestEndpoint.getEndpointCode());
        String endpointName = StringUtils.trimToNull(requestEndpoint.getName());
        String method = StringUtils.upperCase(StringUtils.trimToNull(requestEndpoint.getMethod()));
        String internalPath = StringUtils.trimToNull(requestEndpoint.getInternalPath());
        String targetPath = StringUtils.trimToNull(requestEndpoint.getTargetPath());
        if (StringUtils.isAnyBlank(endpointCode, endpointName, method, internalPath, targetPath)) {
            throw new BusinessException(
                    ErrorCode.PARAMS_ERROR,
                    "endpointCode, name, method, internalPath and targetPath are required"
            );
        }
        QueryWrapper<SensorApiEndpoint> codeQueryWrapper = new QueryWrapper<SensorApiEndpoint>().eq("endpointCode", endpointCode);
        QueryWrapper<SensorApiEndpoint> pathMethodQueryWrapper = new QueryWrapper<SensorApiEndpoint>()
                .eq("internalPath", internalPath)
                .eq("method", method);
        if (requestEndpoint.getId() != null) {
            codeQueryWrapper.ne("id", requestEndpoint.getId());
            pathMethodQueryWrapper.ne("id", requestEndpoint.getId());
        }
        if (sensorApiEndpointService.count(codeQueryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "endpointCode already exists");
        }
        if (sensorApiEndpointService.count(pathMethodQueryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "internalPath + method already exists");
        }
        sensorApiEndpoint.setEndpointCode(endpointCode);
        sensorApiEndpoint.setName(endpointName);
        sensorApiEndpoint.setDescription(StringUtils.trimToNull(requestEndpoint.getDescription()));
        sensorApiEndpoint.setMethod(method);
        sensorApiEndpoint.setInternalPath(internalPath);
        sensorApiEndpoint.setTargetService(StringUtils.defaultIfBlank(
                StringUtils.trimToNull(requestEndpoint.getTargetService()),
                DEFAULT_TARGET_SERVICE
        ));
        sensorApiEndpoint.setTargetPath(targetPath);
        sensorApiEndpoint.setCategory(StringUtils.defaultIfBlank(StringUtils.trimToNull(requestEndpoint.getCategory()), "northbound"));
        sensorApiEndpoint.setProtocolType(StringUtils.defaultIfBlank(
                StringUtils.trimToNull(requestEndpoint.getProtocolType()),
                DEFAULT_PROTOCOL
        ));
        sensorApiEndpoint.setRequestSchema(trimNullableText(
                requestEndpoint.getRequestSchema(),
                oldEndpoint == null ? null : oldEndpoint.getRequestSchema()
        ));
        sensorApiEndpoint.setResponseSchema(trimNullableText(
                requestEndpoint.getResponseSchema(),
                oldEndpoint == null ? null : oldEndpoint.getResponseSchema()
        ));
        sensorApiEndpoint.setStatus(requestEndpoint.getStatus() == null ? 1 : requestEndpoint.getStatus());
        boolean result = isCreate ? sensorApiEndpointService.save(sensorApiEndpoint) : sensorApiEndpointService.updateById(sensorApiEndpoint);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "save sensor api endpoint failed");
        }
        return sensorApiEndpoint;
    }

    private SensorInterfaceBinding saveSensorInterfaceBindingInternal(SensorInterfaceBinding requestBinding, boolean isCreate) {
        if (requestBinding == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SensorInterfaceBinding oldBinding = null;
        if (!isCreate) {
            if (requestBinding.getId() == null || requestBinding.getId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "binding id is required");
            }
            oldBinding = sensorInterfaceBindingService.getById(requestBinding.getId());
            if (oldBinding == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "sensor interface binding not found");
            }
        }
        Long sensorApiEndpointId = requestBinding.getSensorApiEndpointId();
        Long interfaceInfoId = requestBinding.getInterfaceInfoId();
        if (sensorApiEndpointId == null || sensorApiEndpointId <= 0 || interfaceInfoId == null || interfaceInfoId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sensorApiEndpointId and interfaceInfoId are required");
        }
        if (sensorApiEndpointService.getById(sensorApiEndpointId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "sensor api endpoint not found");
        }
        if (interfaceInfoService.getById(interfaceInfoId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "platform interface not found");
        }
        QueryWrapper<SensorInterfaceBinding> duplicateQueryWrapper = new QueryWrapper<SensorInterfaceBinding>()
                .eq("sensorApiEndpointId", sensorApiEndpointId)
                .eq("interfaceInfoId", interfaceInfoId);
        if (requestBinding.getId() != null) {
            duplicateQueryWrapper.ne("id", requestBinding.getId());
        }
        if (sensorInterfaceBindingService.count(duplicateQueryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "binding already exists");
        }
        SensorInterfaceBinding sensorInterfaceBinding = oldBinding == null ? new SensorInterfaceBinding() : oldBinding;
        sensorInterfaceBinding.setSensorApiEndpointId(sensorApiEndpointId);
        sensorInterfaceBinding.setInterfaceInfoId(interfaceInfoId);
        sensorInterfaceBinding.setPublishStrategy(StringUtils.defaultIfBlank(
                StringUtils.trimToNull(requestBinding.getPublishStrategy()),
                DEFAULT_PUBLISH_STRATEGY
        ));
        sensorInterfaceBinding.setAuthStrategy(StringUtils.defaultIfBlank(
                StringUtils.trimToNull(requestBinding.getAuthStrategy()),
                DEFAULT_AUTH_STRATEGY
        ));
        sensorInterfaceBinding.setLimitStrategy(StringUtils.trimToNull(requestBinding.getLimitStrategy()));
        sensorInterfaceBinding.setCacheStrategy(StringUtils.trimToNull(requestBinding.getCacheStrategy()));
        sensorInterfaceBinding.setStatus(requestBinding.getStatus() == null ? 1 : requestBinding.getStatus());
        boolean result = isCreate
                ? sensorInterfaceBindingService.save(sensorInterfaceBinding)
                : sensorInterfaceBindingService.updateById(sensorInterfaceBinding);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "save sensor interface binding failed");
        }
        return sensorInterfaceBinding;
    }

    private SensorRealtimeChannel saveSensorRealtimeChannelInternal(SensorRealtimeChannel requestChannel, boolean isCreate) {
        if (requestChannel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SensorRealtimeChannel oldChannel = null;
        if (!isCreate) {
            if (requestChannel.getId() == null || requestChannel.getId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "realtime channel id is required");
            }
            oldChannel = sensorRealtimeChannelService.getById(requestChannel.getId());
            if (oldChannel == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "sensor realtime channel not found");
            }
        }
        String channelCode = StringUtils.trimToNull(requestChannel.getChannelCode());
        String channelName = StringUtils.trimToNull(requestChannel.getName());
        String wsPath = StringUtils.trimToNull(requestChannel.getWsPath());
        if (StringUtils.isAnyBlank(channelCode, channelName, wsPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "channelCode, name and wsPath are required");
        }
        QueryWrapper<SensorRealtimeChannel> duplicateQueryWrapper =
                new QueryWrapper<SensorRealtimeChannel>().eq("channelCode", channelCode);
        if (requestChannel.getId() != null) {
            duplicateQueryWrapper.ne("id", requestChannel.getId());
        }
        if (sensorRealtimeChannelService.count(duplicateQueryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "channelCode already exists");
        }
        SensorRealtimeChannel sensorRealtimeChannel = oldChannel == null ? new SensorRealtimeChannel() : oldChannel;
        sensorRealtimeChannel.setChannelCode(channelCode);
        sensorRealtimeChannel.setName(channelName);
        sensorRealtimeChannel.setWsPath(wsPath);
        sensorRealtimeChannel.setTopic(StringUtils.trimToNull(requestChannel.getTopic()));
        sensorRealtimeChannel.setDeviceType(StringUtils.trimToNull(requestChannel.getDeviceType()));
        sensorRealtimeChannel.setAuthType(StringUtils.defaultIfBlank(
                StringUtils.trimToNull(requestChannel.getAuthType()),
                DEFAULT_REALTIME_AUTH_TYPE
        ));
        sensorRealtimeChannel.setStatus(requestChannel.getStatus() == null ? 1 : requestChannel.getStatus());
        boolean result = isCreate
                ? sensorRealtimeChannelService.save(sensorRealtimeChannel)
                : sensorRealtimeChannelService.updateById(sensorRealtimeChannel);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "save sensor realtime channel failed");
        }
        return sensorRealtimeChannel;
    }

    private QueryWrapper<SensorDevice> buildSensorDeviceQueryWrapper(SensorDeviceQueryRequest queryRequest) {
        SensorDevice safeQuery = new SensorDevice();
        if (queryRequest != null) {
            BeanUtils.copyProperties(queryRequest, safeQuery);
        }
        QueryWrapper<SensorDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(safeQuery.getDeviceCode()), "deviceCode", safeQuery.getDeviceCode());
        queryWrapper.like(StringUtils.isNotBlank(safeQuery.getDeviceName()), "deviceName", safeQuery.getDeviceName());
        queryWrapper.eq(StringUtils.isNotBlank(safeQuery.getDeviceType()), "deviceType", safeQuery.getDeviceType());
        queryWrapper.eq(safeQuery.getDataSourceId() != null, "dataSourceId", safeQuery.getDataSourceId());
        queryWrapper.eq(safeQuery.getStatus() != null, "status", safeQuery.getStatus());
        queryWrapper.orderByDesc("updateTime");
        return queryWrapper;
    }

    private void syncDerivedRealtimeChannel(String oldRealtimeKey, SensorDevice sensorDevice) {
        if (StringUtils.isNotBlank(oldRealtimeKey)
                && !StringUtils.equals(oldRealtimeKey, sensorDevice.getRealtimeKey())) {
            removeRealtimeChannelByKey(oldRealtimeKey);
        }
        if (StringUtils.isBlank(sensorDevice.getRealtimeKey())) {
            return;
        }
        SensorDataSource defaultSource = getOrCreateDefaultDataSource();
        String channelCode = buildRealtimeChannelCode(sensorDevice.getRealtimeKey());
        SensorRealtimeChannel channel = sensorRealtimeChannelService.getOne(
                new QueryWrapper<SensorRealtimeChannel>().eq("channelCode", channelCode),
                false
        );
        boolean isNew = channel == null;
        if (isNew) {
            channel = new SensorRealtimeChannel();
        }
        channel.setChannelCode(channelCode);
        channel.setName(buildRealtimeChannelName(sensorDevice));
        channel.setWsPath(StringUtils.defaultIfBlank(defaultSource.getWsUrl(), buildDefaultWsUrl()));
        channel.setTopic(sensorDevice.getRealtimeKey());
        channel.setDeviceType(sensorDevice.getDeviceType());
        channel.setAuthType(DEFAULT_REALTIME_AUTH_TYPE);
        channel.setStatus(sensorDevice.getStatus() == null ? 1 : sensorDevice.getStatus());
        if (isNew) {
            sensorRealtimeChannelService.save(channel);
        } else {
            sensorRealtimeChannelService.updateById(channel);
        }
    }

    private String trimNullableText(String currentValue, String fallbackValue) {
        String trimmed = StringUtils.trimToNull(currentValue);
        return trimmed == null ? fallbackValue : trimmed;
    }

    private SensorDataSource getOrCreateDefaultDataSource() {
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
            if (!StringUtils.equals(dataSource.getType(), DEFAULT_SOURCE_TYPE)) {
                dataSource.setType(DEFAULT_SOURCE_TYPE);
                changed = true;
            }
            if (!StringUtils.equals(dataSource.getBaseUrl(), platformPublicBaseUrl)) {
                dataSource.setBaseUrl(platformPublicBaseUrl);
                changed = true;
            }
            if (!StringUtils.equals(dataSource.getWsUrl(), buildDefaultWsUrl())) {
                dataSource.setWsUrl(buildDefaultWsUrl());
                changed = true;
            }
            if (dataSource.getStatus() == null || dataSource.getStatus() != 1) {
                dataSource.setStatus(1);
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
        dataSource.setType(DEFAULT_SOURCE_TYPE);
        dataSource.setBaseUrl(platformPublicBaseUrl);
        dataSource.setWsUrl(buildDefaultWsUrl());
        dataSource.setStatus(1);
        dataSource.setConfigJson("{\"managedBy\":\"SensorHub\",\"phase\":\"phase4-brand-cleanup\"}");
        boolean saved = sensorDataSourceService.save(dataSource);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "failed to initialize default sensor data source");
        }
        return dataSource;
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

    private void syncNorthboundOnlyEndpoints(Map<String, SensorWorkspaceInterfaceVO> workspaceInterfaceMap,
                                             Set<String> processedKeys,
                                             List<String> created,
                                             List<String> updated) {
        for (SensorWorkspaceInterfaceVO workspaceInterface : workspaceInterfaceMap.values()) {
            if (workspaceInterface == null
                    || StringUtils.isAnyBlank(workspaceInterface.getMethod(), workspaceInterface.getTargetPath())) {
                continue;
            }
            String methodPathKey = buildMethodPathKey(workspaceInterface.getMethod(), workspaceInterface.getTargetPath());
            if (processedKeys.contains(methodPathKey)) {
                continue;
            }
            processedKeys.add(methodPathKey);
            String endpointCode = buildEndpointCode(workspaceInterface.getMethod(), workspaceInterface.getTargetPath());
            SensorApiEndpoint sensorApiEndpoint = findExistingApiEndpoint(
                    workspaceInterface.getMethod(),
                    workspaceInterface.getGatewayPath(),
                    workspaceInterface.getTargetPath(),
                    endpointCode
            );
            boolean isNew = sensorApiEndpoint == null;
            if (isNew) {
                sensorApiEndpoint = new SensorApiEndpoint();
            }
            sensorApiEndpoint.setEndpointCode(endpointCode);
            sensorApiEndpoint.setName(workspaceInterface.getName());
            sensorApiEndpoint.setDescription(workspaceInterface.getDescription());
            sensorApiEndpoint.setMethod(StringUtils.upperCase(workspaceInterface.getMethod()));
            sensorApiEndpoint.setInternalPath(workspaceInterface.getGatewayPath());
            sensorApiEndpoint.setTargetService(DEFAULT_TARGET_SERVICE);
            sensorApiEndpoint.setTargetPath(workspaceInterface.getTargetPath());
            sensorApiEndpoint.setCategory("northbound");
            sensorApiEndpoint.setProtocolType(DEFAULT_PROTOCOL);
            sensorApiEndpoint.setRequestSchema(workspaceInterface.getRequestParams());
            sensorApiEndpoint.setResponseSchema(workspaceInterface.getResponseHeader());
            sensorApiEndpoint.setStatus(workspaceInterface.getStatus() == null ? 1 : workspaceInterface.getStatus());
            if (isNew) {
                sensorApiEndpointService.save(sensorApiEndpoint);
                created.add(endpointCode);
            } else {
                sensorApiEndpointService.updateById(sensorApiEndpoint);
                updated.add(endpointCode);
            }
        }
    }

    private Map<String, SensorWorkspaceInterfaceVO> getWorkspaceInterfaceMapByTargetPath() {
        List<SensorWorkspaceInterfaceVO> workspaceInterfaceList = sensorWorkspaceService.listNorthboundInterfaces();
        Map<String, SensorWorkspaceInterfaceVO> resultMap = new HashMap<>();
        for (SensorWorkspaceInterfaceVO item : workspaceInterfaceList) {
            resultMap.put(buildMethodPathKey(item.getMethod(), item.getTargetPath()), item);
        }
        return resultMap;
    }

    private SensorRegistrySyncResultVO buildSyncResult(String scope,
                                                       int total,
                                                       List<String> created,
                                                       List<String> updated,
                                                       List<String> skipped) {
        SensorRegistrySyncResultVO resultVO = new SensorRegistrySyncResultVO();
        resultVO.setScope(scope);
        resultVO.setTotal(total);
        resultVO.setCreatedCount(created.size());
        resultVO.setUpdatedCount(updated.size());
        resultVO.setSkippedCount(skipped.size());
        resultVO.setCreatedKeys(created);
        resultVO.setUpdatedKeys(updated);
        resultVO.setSkippedKeys(skipped);
        return resultVO;
    }

    private String buildEndpointCode(String method, String path) {
        String raw = ENDPOINT_CODE_PREFIX + "_" + method + "_" + path;
        return raw.replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "").toUpperCase();
    }

    private String buildMethodPathKey(String method, String path) {
        return StringUtils.upperCase(method) + "#" + path;
    }

    private String buildRealtimeChannelCode(String realtimeKey) {
        String raw = CHANNEL_CODE_PREFIX + "_" + realtimeKey;
        return raw.replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "").toUpperCase();
    }

    private SensorApiEndpoint findExistingApiEndpoint(String method,
                                                      String gatewayPath,
                                                      String targetPath,
                                                      String endpointCode) {
        SensorApiEndpoint sensorApiEndpoint = sensorApiEndpointService.getOne(
                new QueryWrapper<SensorApiEndpoint>()
                        .eq("internalPath", gatewayPath)
                        .eq("method", StringUtils.upperCase(method)),
                false
        );
        if (sensorApiEndpoint != null) {
            return sensorApiEndpoint;
        }
        sensorApiEndpoint = sensorApiEndpointService.getOne(
                new QueryWrapper<SensorApiEndpoint>().eq("endpointCode", endpointCode),
                false
        );
        if (sensorApiEndpoint != null) {
            return sensorApiEndpoint;
        }
        return null;
    }

    private void removeRealtimeChannelByKey(String realtimeKey) {
        sensorRealtimeChannelService.remove(
                new QueryWrapper<SensorRealtimeChannel>()
                        .eq("channelCode", buildRealtimeChannelCode(realtimeKey))
        );
    }

    private String buildRealtimeChannelName(SensorDevice device) {
        String baseName = StringUtils.defaultIfBlank(device.getDeviceName(), device.getDeviceType());
        return baseName + " Realtime";
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

    private boolean toBooleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() > 0;
        }
        return StringUtils.equalsAnyIgnoreCase(String.valueOf(value), "true", "1");
    }

    private String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        JsonElement jsonElement = gson.toJsonTree(value);
        return gson.toJson(jsonElement);
    }
}
