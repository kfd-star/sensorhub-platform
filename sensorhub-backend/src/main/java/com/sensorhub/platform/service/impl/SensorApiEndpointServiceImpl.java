package com.sensorhub.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sensorhub.platform.mapper.SensorApiEndpointMapper;
import com.sensorhub.platform.model.entity.SensorApiEndpoint;
import com.sensorhub.platform.service.SensorApiEndpointService;
import org.springframework.stereotype.Service;

@Service
public class SensorApiEndpointServiceImpl extends ServiceImpl<SensorApiEndpointMapper, SensorApiEndpoint>
        implements SensorApiEndpointService {
}
