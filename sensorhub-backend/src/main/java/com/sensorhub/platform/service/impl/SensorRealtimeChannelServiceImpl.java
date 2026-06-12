package com.sensorhub.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sensorhub.platform.mapper.SensorRealtimeChannelMapper;
import com.sensorhub.platform.model.entity.SensorRealtimeChannel;
import com.sensorhub.platform.service.SensorRealtimeChannelService;
import org.springframework.stereotype.Service;

@Service
public class SensorRealtimeChannelServiceImpl extends ServiceImpl<SensorRealtimeChannelMapper, SensorRealtimeChannel>
        implements SensorRealtimeChannelService {
}
