package com.sensorhub.platform.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class SensorRegistryDashboardVO implements Serializable {

    private Long dataSourceCount;

    private Long deviceCount;

    private Long endpointCount;

    private Long bindingCount;

    private Long realtimeChannelCount;

    private Long platformInterfaceCount;

    private Long publishedInterfaceCount;

    private String defaultDataSourceCode;

    private static final long serialVersionUID = 1L;
}
