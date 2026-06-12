package com.sensorhub.platform.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Sync result for the sensor workspace catalog.
 */
@Data
public class SensorWorkspaceCatalogSyncResultVO implements Serializable {

    private Integer total;

    private Integer createdCount;

    private Integer updatedCount;

    private List<String> createdNames;

    private List<String> updatedNames;

    private static final long serialVersionUID = 1L;
}
