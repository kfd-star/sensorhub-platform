package com.sensorhub.platform.model.dto.sensor;

import com.sensorhub.platform.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SensorDeviceQueryRequest extends PageRequest implements Serializable {

    private String deviceCode;

    private String deviceName;

    private String deviceType;

    private Long dataSourceId;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
