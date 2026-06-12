package com.sensorhub.platform.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class SensorRegistryBootstrapResultVO implements Serializable {

    private String dataSourceCode;

    private SensorWorkspaceCatalogSyncResultVO interfaceCatalogSync;

    private SensorRegistrySyncResultVO deviceSync;

    private SensorRegistrySyncResultVO endpointSync;

    private SensorRegistrySyncResultVO bindingSync;

    private SensorRegistrySyncResultVO realtimeChannelSync;

    private static final long serialVersionUID = 1L;
}
