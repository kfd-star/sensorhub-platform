package com.sensorhub.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sensorhub.platform.annotation.AuthCheck;
import com.sensorhub.platform.common.BaseResponse;
import com.sensorhub.platform.common.DeleteRequest;
import com.sensorhub.platform.common.ErrorCode;
import com.sensorhub.platform.common.ResultUtils;
import com.sensorhub.platform.constant.UserConstant;
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
import com.sensorhub.platform.model.vo.SensorInterfaceBindingVO;
import com.sensorhub.platform.model.vo.SensorRegistryBootstrapResultVO;
import com.sensorhub.platform.model.vo.SensorRegistryDashboardVO;
import com.sensorhub.platform.model.vo.SensorRegistrySyncResultVO;
import com.sensorhub.platform.service.SensorRegistryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/sensorRegistry")
public class SensorRegistryController {

    @Resource
    private SensorRegistryService sensorRegistryService;

    @GetMapping("/dashboard")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SensorRegistryDashboardVO> getDashboard() {
        return ResultUtils.success(sensorRegistryService.getDashboard());
    }

    @PostMapping("/bootstrap")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SensorRegistryBootstrapResultVO> bootstrapRegistry() {
        return ResultUtils.success(sensorRegistryService.bootstrapRegistry());
    }

    @PostMapping("/sync/devices")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SensorRegistrySyncResultVO> syncDevices() {
        return ResultUtils.success(sensorRegistryService.syncDevicesFromWorkspace());
    }

    @PostMapping("/sync/endpoints")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SensorRegistrySyncResultVO> syncEndpoints() {
        return ResultUtils.success(sensorRegistryService.syncApiEndpointsFromWorkspace());
    }

    @PostMapping("/sync/bindings")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SensorRegistrySyncResultVO> syncBindings() {
        return ResultUtils.success(sensorRegistryService.syncInterfaceBindings());
    }

    @PostMapping("/sync/realtimeChannels")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SensorRegistrySyncResultVO> syncRealtimeChannels() {
        return ResultUtils.success(sensorRegistryService.syncRealtimeChannels());
    }

    @GetMapping("/dataSource/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<SensorDataSource>> listDataSources() {
        return ResultUtils.success(sensorRegistryService.listDataSources());
    }

    @GetMapping("/device/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SensorDevice>> listDevicesByPage(SensorDeviceQueryRequest queryRequest) {
        return ResultUtils.success(sensorRegistryService.listDevicesByPage(queryRequest));
    }

    @GetMapping("/device/public/list")
    public BaseResponse<List<SensorDevice>> listPublicDevices(SensorDeviceQueryRequest queryRequest) {
        return ResultUtils.success(sensorRegistryService.listDevices(queryRequest));
    }

    @GetMapping("/apiEndpoint/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SensorApiEndpoint>> listApiEndpointsByPage(SensorApiEndpointQueryRequest queryRequest) {
        return ResultUtils.success(sensorRegistryService.listApiEndpointsByPage(queryRequest));
    }

    @GetMapping("/binding/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SensorInterfaceBindingVO>> listBindingsByPage(SensorInterfaceBindingQueryRequest queryRequest) {
        return ResultUtils.success(sensorRegistryService.listBindingsByPage(queryRequest));
    }

    @GetMapping("/realtimeChannel/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SensorRealtimeChannel>> listRealtimeChannelsByPage(SensorRealtimeChannelQueryRequest queryRequest) {
        return ResultUtils.success(sensorRegistryService.listRealtimeChannelsByPage(queryRequest));
    }

    @PostMapping("/device/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addSensorDevice(@RequestBody SensorDevice sensorDevice) {
        if (sensorDevice == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.addSensorDevice(sensorDevice));
    }

    @PostMapping("/device/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSensorDevice(@RequestBody SensorDevice sensorDevice) {
        if (sensorDevice == null || sensorDevice.getId() == null || sensorDevice.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.updateSensorDevice(sensorDevice));
    }

    @PostMapping("/device/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteSensorDevice(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.deleteSensorDevice(deleteRequest.getId()));
    }

    @PostMapping("/apiEndpoint/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addSensorApiEndpoint(@RequestBody SensorApiEndpoint sensorApiEndpoint) {
        if (sensorApiEndpoint == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.addSensorApiEndpoint(sensorApiEndpoint));
    }

    @PostMapping("/apiEndpoint/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSensorApiEndpoint(@RequestBody SensorApiEndpoint sensorApiEndpoint) {
        if (sensorApiEndpoint == null || sensorApiEndpoint.getId() == null || sensorApiEndpoint.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.updateSensorApiEndpoint(sensorApiEndpoint));
    }

    @PostMapping("/apiEndpoint/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteSensorApiEndpoint(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.deleteSensorApiEndpoint(deleteRequest.getId()));
    }

    @PostMapping("/binding/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addSensorInterfaceBinding(@RequestBody SensorInterfaceBinding sensorInterfaceBinding) {
        if (sensorInterfaceBinding == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.addSensorInterfaceBinding(sensorInterfaceBinding));
    }

    @PostMapping("/binding/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSensorInterfaceBinding(@RequestBody SensorInterfaceBinding sensorInterfaceBinding) {
        if (sensorInterfaceBinding == null
                || sensorInterfaceBinding.getId() == null
                || sensorInterfaceBinding.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.updateSensorInterfaceBinding(sensorInterfaceBinding));
    }

    @PostMapping("/binding/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteSensorInterfaceBinding(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.deleteSensorInterfaceBinding(deleteRequest.getId()));
    }

    @PostMapping("/realtimeChannel/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addSensorRealtimeChannel(@RequestBody SensorRealtimeChannel sensorRealtimeChannel) {
        if (sensorRealtimeChannel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.addSensorRealtimeChannel(sensorRealtimeChannel));
    }

    @PostMapping("/realtimeChannel/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSensorRealtimeChannel(@RequestBody SensorRealtimeChannel sensorRealtimeChannel) {
        if (sensorRealtimeChannel == null
                || sensorRealtimeChannel.getId() == null
                || sensorRealtimeChannel.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.updateSensorRealtimeChannel(sensorRealtimeChannel));
    }

    @PostMapping("/realtimeChannel/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteSensorRealtimeChannel(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sensorRegistryService.deleteSensorRealtimeChannel(deleteRequest.getId()));
    }
}
