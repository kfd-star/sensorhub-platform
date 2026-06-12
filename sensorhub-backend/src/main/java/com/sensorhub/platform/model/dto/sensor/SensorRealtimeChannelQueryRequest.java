package com.sensorhub.platform.model.dto.sensor;

import com.sensorhub.platform.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SensorRealtimeChannelQueryRequest extends PageRequest implements Serializable {

    private String channelCode;

    private String name;

    private String deviceType;

    private String authType;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
