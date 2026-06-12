package com.sensorhub.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sensorhub.platform.mapper.SensorDeviceMapper;
import com.sensorhub.platform.model.entity.SensorDevice;
import com.sensorhub.platform.service.SensorDeviceService;
import org.springframework.stereotype.Service;

@Service
public class SensorDeviceServiceImpl extends ServiceImpl<SensorDeviceMapper, SensorDevice>
        implements SensorDeviceService {
}
