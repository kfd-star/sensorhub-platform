package com.sensorhub.platform.integration.workspace;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Internal catalog item for sensor workspace interfaces.
 */
@Data
@AllArgsConstructor
public class SensorWorkspaceInterfaceDefinition {

    private String name;

    private String description;

    private String method;

    private String gatewayPath;

    private String platformUrl;

    private String targetPath;

    private String requestParams;

    private String requestHeader;

    private String responseHeader;
}
