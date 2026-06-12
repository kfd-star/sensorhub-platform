package com.sensorhub.platform.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class SensorInterfaceBindingVO implements Serializable {

    private Long id;

    private Long sensorApiEndpointId;

    private String endpointCode;

    private String endpointName;

    private String internalPath;

    private String method;

    private Long interfaceInfoId;

    private String interfaceName;

    private String interfaceUrl;

    private Integer interfaceStatus;

    private String publishStrategy;

    private String authStrategy;

    private String limitStrategy;

    private String cacheStrategy;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
