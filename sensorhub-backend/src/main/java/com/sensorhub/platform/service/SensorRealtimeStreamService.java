package com.sensorhub.platform.service;

import com.sensorhub.platform.model.vo.SensorRealtimeMessageVO;

import java.util.List;

public interface SensorRealtimeStreamService {

    /**
     * Build the latest realtime telemetry snapshot for all enabled channels.
     */
    List<SensorRealtimeMessageVO> listLatestMessages();
}
