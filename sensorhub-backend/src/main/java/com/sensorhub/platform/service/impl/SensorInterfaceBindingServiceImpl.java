package com.sensorhub.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sensorhub.platform.mapper.SensorInterfaceBindingMapper;
import com.sensorhub.platform.model.entity.SensorInterfaceBinding;
import com.sensorhub.platform.service.SensorInterfaceBindingService;
import org.springframework.stereotype.Service;

@Service
public class SensorInterfaceBindingServiceImpl extends ServiceImpl<SensorInterfaceBindingMapper, SensorInterfaceBinding>
        implements SensorInterfaceBindingService {
}
