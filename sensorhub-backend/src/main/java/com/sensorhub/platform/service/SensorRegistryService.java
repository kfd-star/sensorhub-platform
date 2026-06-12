package com.sensorhub.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sensorhub.platform.model.dto.sensor.SensorApiEndpointQueryRequest;
import com.sensorhub.platform.model.dto.sensor.SensorDeviceQueryRequest;
import com.sensorhub.platform.model.dto.sensor.SensorInterfaceBindingQueryRequest;
import com.sensorhub.platform.model.dto.sensor.SensorRealtimeChannelQueryRequest;
import com.sensorhub.platform.model.entity.SensorApiEndpoint;
import com.sensorhub.platform.model.entity.SensorDataSource;
import com.sensorhub.platform.model.entity.SensorDevice;
import com.sensorhub.platform.model.entity.SensorInterfaceBinding;
import com.sensorhub.platform.model.entity.SensorRealtimeChannel;
import com.sensorhub.platform.model.vo.SensorInterfaceBindingVO;
import com.sensorhub.platform.model.vo.SensorRegistryBootstrapResultVO;
import com.sensorhub.platform.model.vo.SensorRegistryDashboardVO;
import com.sensorhub.platform.model.vo.SensorRegistrySyncResultVO;

import java.util.List;

public interface SensorRegistryService {

    SensorRegistryDashboardVO getDashboard();

    SensorRegistryBootstrapResultVO bootstrapRegistry();

    SensorRegistrySyncResultVO syncDevicesFromWorkspace();

    SensorRegistrySyncResultVO syncApiEndpointsFromWorkspace();

    SensorRegistrySyncResultVO syncInterfaceBindings();

    SensorRegistrySyncResultVO syncRealtimeChannels();

    List<SensorDataSource> listDataSources();

    List<SensorDevice> listDevices(SensorDeviceQueryRequest queryRequest);

    Page<SensorDevice> listDevicesByPage(SensorDeviceQueryRequest queryRequest);

    Page<SensorApiEndpoint> listApiEndpointsByPage(SensorApiEndpointQueryRequest queryRequest);

    Page<SensorInterfaceBindingVO> listBindingsByPage(SensorInterfaceBindingQueryRequest queryRequest);

    Page<SensorRealtimeChannel> listRealtimeChannelsByPage(SensorRealtimeChannelQueryRequest queryRequest);

    Long addSensorDevice(SensorDevice sensorDevice);

    boolean updateSensorDevice(SensorDevice sensorDevice);

    boolean deleteSensorDevice(Long id);

    Long addSensorApiEndpoint(SensorApiEndpoint sensorApiEndpoint);

    boolean updateSensorApiEndpoint(SensorApiEndpoint sensorApiEndpoint);

    boolean deleteSensorApiEndpoint(Long id);

    Long addSensorInterfaceBinding(SensorInterfaceBinding sensorInterfaceBinding);

    boolean updateSensorInterfaceBinding(SensorInterfaceBinding sensorInterfaceBinding);

    boolean deleteSensorInterfaceBinding(Long id);

    Long addSensorRealtimeChannel(SensorRealtimeChannel sensorRealtimeChannel);

    boolean updateSensorRealtimeChannel(SensorRealtimeChannel sensorRealtimeChannel);

    boolean deleteSensorRealtimeChannel(Long id);
}
