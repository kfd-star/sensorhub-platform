package com.sensorhub.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sensorhub.platform.mapper.SensorDataSourceMapper;
import com.sensorhub.platform.model.entity.SensorDataSource;
import com.sensorhub.platform.service.SensorDataSourceService;
import org.springframework.stereotype.Service;

@Service
public class SensorDataSourceServiceImpl extends ServiceImpl<SensorDataSourceMapper, SensorDataSource>
        implements SensorDataSourceService {
}
