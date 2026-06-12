package com.sensorhub.platform.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Sensor workspace interface view object.
 */
@Data
public class SensorWorkspaceInterfaceVO implements Serializable {

    private String name;

    private String description;

    private String method;

    private String gatewayPath;

    private String platformUrl;

    private String targetPath;

    private String requestParams;

    private String requestHeader;

    private String responseHeader;

    private Boolean synced;

    private Long interfaceInfoId;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
