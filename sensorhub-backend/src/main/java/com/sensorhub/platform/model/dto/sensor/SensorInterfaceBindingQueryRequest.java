package com.sensorhub.platform.model.dto.sensor;

import com.sensorhub.platform.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SensorInterfaceBindingQueryRequest extends PageRequest implements Serializable {

    private Long sensorApiEndpointId;

    private Long interfaceInfoId;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
