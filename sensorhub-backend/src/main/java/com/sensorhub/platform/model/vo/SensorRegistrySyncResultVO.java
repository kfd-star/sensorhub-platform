package com.sensorhub.platform.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SensorRegistrySyncResultVO implements Serializable {

    private String scope;

    private Integer total;

    private Integer createdCount;

    private Integer updatedCount;

    private Integer skippedCount;

    private List<String> createdKeys;

    private List<String> updatedKeys;

    private List<String> skippedKeys;

    private static final long serialVersionUID = 1L;
}
