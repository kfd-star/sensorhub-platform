package com.sensorhub.platform.controller;

import com.sensorhub.platform.annotation.AuthCheck;
import com.sensorhub.platform.common.BaseResponse;
import com.sensorhub.platform.common.ResultUtils;
import com.sensorhub.platform.constant.UserConstant;
import com.sensorhub.platform.model.vo.SensorWorkspaceCatalogSyncResultVO;
import com.sensorhub.platform.model.vo.SensorWorkspaceInterfaceVO;
import com.sensorhub.platform.service.SensorWorkspaceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Local sensor workspace controller.
 */
@RestController
@RequestMapping("/sensorWorkspace")
public class SensorWorkspaceController {

    @Resource
    private SensorWorkspaceService sensorWorkspaceService;

    @GetMapping("/catalog")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<SensorWorkspaceInterfaceVO>> getCatalog() {
        return ResultUtils.success(sensorWorkspaceService.listNorthboundInterfaces());
    }

    @GetMapping("/overview")
    public BaseResponse<Map<String, Object>> getOverview() {
        return ResultUtils.success(sensorWorkspaceService.getWorkspaceOverview());
    }

    @GetMapping("/devices")
    public BaseResponse<List<Map<String, Object>>> getDevices() {
        return ResultUtils.success(sensorWorkspaceService.listSensorDevices());
    }

    @GetMapping("/realtimeWorkspace")
    public BaseResponse<Map<String, Object>> getRealtimeWorkspace() {
        return ResultUtils.success(sensorWorkspaceService.getRealtimeWorkspace());
    }

    @PostMapping("/devices")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Map<String, Object>> createDevice(@RequestBody Map<String, Object> devicePayload) {
        return ResultUtils.success(sensorWorkspaceService.createSensorDevice(devicePayload));
    }

    @PutMapping("/devices/{deviceId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Map<String, Object>> updateDevice(
            @PathVariable("deviceId") String deviceId,
            @RequestBody Map<String, Object> devicePayload
    ) {
        return ResultUtils.success(sensorWorkspaceService.updateSensorDevice(deviceId, devicePayload));
    }

    @DeleteMapping("/devices/{deviceId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Map<String, Object>> deleteDevice(@PathVariable("deviceId") String deviceId) {
        return ResultUtils.success(sensorWorkspaceService.deleteSensorDevice(deviceId));
    }

    @PostMapping("/devices/test")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Map<String, Object>> testDevice(@RequestBody Map<String, Object> devicePayload) {
        return ResultUtils.success(sensorWorkspaceService.testSensorDevice(devicePayload));
    }

    @GetMapping("/realtime")
    public BaseResponse<Map<String, Object>> getRealtimeSnapshot(
            @RequestParam(value = "limit", required = false, defaultValue = "5") Integer limit
    ) {
        return ResultUtils.success(sensorWorkspaceService.getRealtimeSnapshot(limit));
    }

    @GetMapping("/search")
    public BaseResponse<Map<String, Object>> searchDataset(
            @RequestParam("dataset") String dataset,
            @RequestParam(value = "deviceToken", required = false) String deviceToken,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit
    ) {
        return ResultUtils.success(
                sensorWorkspaceService.searchDataset(dataset, deviceToken, startDate, endDate, limit)
        );
    }

    @PostMapping("/sync")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SensorWorkspaceCatalogSyncResultVO> syncCatalog() {
        return ResultUtils.success(sensorWorkspaceService.syncNorthboundInterfaces());
    }
}
