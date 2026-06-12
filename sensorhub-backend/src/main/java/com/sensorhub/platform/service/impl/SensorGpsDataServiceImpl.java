package com.sensorhub.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sensorhub.platform.mapper.SensorGpsDataMapper;
import com.sensorhub.platform.model.entity.SensorGpsData;
import com.sensorhub.platform.service.SensorGpsDataService;
import org.springframework.stereotype.Service;

@Service
public class SensorGpsDataServiceImpl extends ServiceImpl<SensorGpsDataMapper, SensorGpsData>
        implements SensorGpsDataService {
}
