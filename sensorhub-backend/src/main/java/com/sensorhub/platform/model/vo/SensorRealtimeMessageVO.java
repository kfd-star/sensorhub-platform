package com.sensorhub.platform.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class SensorRealtimeMessageVO implements Serializable {

    private String type;

    private String device;

    @JsonProperty("device_name")
    private String deviceName;

    private String timestamp;

    private Map<String, Object> data;

    private static final long serialVersionUID = 1L;
}
