package com.sensorhub.platform.model.dto.sensor;

import com.sensorhub.platform.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SensorApiEndpointQueryRequest extends PageRequest implements Serializable {

    private String endpointCode;

    private String name;

    private String method;

    private String targetService;

    private String category;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
