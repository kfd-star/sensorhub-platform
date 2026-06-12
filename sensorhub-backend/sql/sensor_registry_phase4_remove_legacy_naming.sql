-- Phase 4 migration: physically remove deprecated legacy naming from sensor registry schema and data.
-- Run this script in the target MySQL database, for example: sensorhub.

SET @db_name = DATABASE();

SET @legacy_device_id_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = @db_name
      AND table_name = 'sensor_device'
      AND column_name = 'legacyDeviceId'
);

SET @deprecated_device_id_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = @db_name
      AND table_name = 'sensor_device'
      AND column_name = 'sdsDeviceId'
);

SET @sql = IF(
    @legacy_device_id_exists = 0 AND @deprecated_device_id_exists > 0,
    'ALTER TABLE sensor_device CHANGE COLUMN sdsDeviceId legacyDeviceId VARCHAR(128) DEFAULT NULL',
    'SELECT ''skip rename sensor_device.sdsDeviceId'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @legacy_device_id_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = @db_name
      AND table_name = 'sensor_device'
      AND column_name = 'legacyDeviceId'
);

SET @sql = IF(
    @legacy_device_id_exists = 0,
    'ALTER TABLE sensor_device ADD COLUMN legacyDeviceId VARCHAR(128) DEFAULT NULL AFTER dataSourceId',
    'SELECT ''skip add sensor_device.legacyDeviceId'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @legacy_device_id_index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = @db_name
      AND table_name = 'sensor_device'
      AND index_name = 'idx_sensor_device_legacy_id'
);

SET @sql = IF(
    @legacy_device_id_index_exists = 0,
    'ALTER TABLE sensor_device ADD KEY idx_sensor_device_legacy_id (legacyDeviceId)',
    'SELECT ''skip add idx_sensor_device_legacy_id'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sensor_device
SET legacyDeviceId = deviceCode,
    updateTime = CURRENT_TIMESTAMP
WHERE isDelete = 0
  AND (legacyDeviceId IS NULL OR legacyDeviceId = '');

SET @current_source_id = (
    SELECT id
    FROM sensor_data_source
    WHERE code = 'API_EXAMPLE_LOCAL'
      AND isDelete = 0
    ORDER BY id ASC
    LIMIT 1
);

SET @deprecated_source_id = (
    SELECT id
    FROM sensor_data_source
    WHERE code = 'SDS_NODE_LOCAL'
      AND isDelete = 0
    ORDER BY id ASC
    LIMIT 1
);

UPDATE sensor_device
SET dataSourceId = @current_source_id,
    updateTime = CURRENT_TIMESTAMP
WHERE @current_source_id IS NOT NULL
  AND @deprecated_source_id IS NOT NULL
  AND isDelete = 0
  AND dataSourceId = @deprecated_source_id;

UPDATE sensor_data_source
SET isDelete = 1,
    updateTime = CURRENT_TIMESTAMP
WHERE @current_source_id IS NOT NULL
  AND @deprecated_source_id IS NOT NULL
  AND id = @deprecated_source_id
  AND isDelete = 0;

UPDATE sensor_data_source
SET code = 'API_EXAMPLE_LOCAL',
    name = 'SensorHub Local Data Source',
    updateTime = CURRENT_TIMESTAMP
WHERE @current_source_id IS NULL
  AND @deprecated_source_id IS NOT NULL
  AND id = @deprecated_source_id
  AND isDelete = 0;

UPDATE sensor_api_endpoint AS current_endpoint
SET current_endpoint.endpointCode = REPLACE(current_endpoint.endpointCode, 'SDS_', 'SENSOR_'),
    current_endpoint.updateTime = CURRENT_TIMESTAMP
WHERE current_endpoint.isDelete = 0
  AND LEFT(current_endpoint.endpointCode, 4) = 'SDS_'
  AND NOT EXISTS (
    SELECT 1
    FROM (
      SELECT endpointCode
      FROM sensor_api_endpoint
      WHERE isDelete = 0
    ) AS existing_endpoint
    WHERE existing_endpoint.endpointCode = REPLACE(current_endpoint.endpointCode, 'SDS_', 'SENSOR_')
  );

UPDATE sensor_realtime_channel AS current_channel
SET current_channel.channelCode = REPLACE(current_channel.channelCode, 'SDS_WS_', 'SENSOR_WS_'),
    current_channel.updateTime = CURRENT_TIMESTAMP
WHERE current_channel.isDelete = 0
  AND LEFT(current_channel.channelCode, 7) = 'SDS_WS_'
  AND NOT EXISTS (
    SELECT 1
    FROM (
      SELECT channelCode
      FROM sensor_realtime_channel
      WHERE isDelete = 0
    ) AS existing_channel
    WHERE existing_channel.channelCode = REPLACE(current_channel.channelCode, 'SDS_WS_', 'SENSOR_WS_')
  );
