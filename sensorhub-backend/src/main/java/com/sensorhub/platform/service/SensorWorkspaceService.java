package com.sensorhub.platform.service;

import com.sensorhub.platform.model.vo.SensorWorkspaceCatalogSyncResultVO;
import com.sensorhub.platform.model.vo.SensorWorkspaceInterfaceVO;

import java.util.List;
import java.util.Map;

/**
 * Sensor workspace service.
 */
public interface SensorWorkspaceService {

    /**
     * List the standard northbound interfaces exposed from the local workspace.
     *
     * @return catalog
     */
    List<SensorWorkspaceInterfaceVO> listNorthboundInterfaces();

    /**
     * Sync the standard sensor workspace catalog into interface_info.
     *
     * @return sync result
     */
    SensorWorkspaceCatalogSyncResultVO syncNorthboundInterfaces();

    /**
     * Build the workspace overview inside SensorHub Open Platform.
     *
     * @return overview payload
     */
    Map<String, Object> getWorkspaceOverview();

    /**
     * Query the current managed device registry.
     *
     * @return device list
     */
    List<Map<String, Object>> listSensorDevices();

    /**
     * Build realtime workspace config for the unified frontend.
     *
     * @return workspace config
     */
    Map<String, Object> getRealtimeWorkspace();

    /**
     * Create a device in the managed registry.
     *
     * @param devicePayload request payload
     * @return downstream response
     */
    Map<String, Object> createSensorDevice(Map<String, Object> devicePayload);

    /**
     * Update a device in the managed registry.
     *
     * @param deviceId device id
     * @param devicePayload request payload
     * @return downstream response
     */
    Map<String, Object> updateSensorDevice(String deviceId, Map<String, Object> devicePayload);

    /**
     * Delete a device in the managed registry.
     *
     * @param deviceId device id
     * @return downstream response
     */
    Map<String, Object> deleteSensorDevice(String deviceId);

    /**
     * Test device connectivity through its data endpoint.
     *
     * @param devicePayload device payload
     * @return connectivity result
     */
    Map<String, Object> testSensorDevice(Map<String, Object> devicePayload);

    /**
     * Query the latest records for each sensor dataset.
     *
     * @param limit max records per dataset
     * @return realtime snapshot
     */
    Map<String, Object> getRealtimeSnapshot(Integer limit);

    /**
     * Search a single sensor dataset.
     *
     * @param datasetCode dataset code
     * @param deviceToken optional device token
     * @param startDate optional yyyy-MM-dd
     * @param endDate optional yyyy-MM-dd
     * @param limit max records
     * @return search result
     */
    Map<String, Object> searchDataset(
            String datasetCode,
            String deviceToken,
            String startDate,
            String endDate,
            Integer limit
    );
}
