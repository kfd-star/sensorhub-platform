USE sensorhub;

UPDATE user_interface_info
SET totalNum = 0, leftNum = 999999
WHERE userId = 2 AND interfaceInfoId = 2 AND isDelete = 0;

UPDATE user_interface_info target
JOIN (
    SELECT MIN(id) AS keepId,
           userId,
           interfaceInfoId,
           isDelete,
           MAX(totalNum) AS totalNum,
           MAX(leftNum) AS leftNum,
           MAX(status) AS status
    FROM user_interface_info
    GROUP BY userId, interfaceInfoId, isDelete
) dedup
ON target.id = dedup.keepId
SET target.totalNum = dedup.totalNum,
    target.leftNum = dedup.leftNum,
    target.status = dedup.status;

DELETE target
FROM user_interface_info target
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY userId, interfaceInfoId, isDelete ORDER BY id) AS rowNum
    FROM user_interface_info
) duplicates
ON target.id = duplicates.id
WHERE duplicates.rowNum > 1;

SET @index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user_interface_info'
      AND index_name = 'uk_user_interface_info_user_interface'
);

SET @create_index_sql = IF(
    @index_exists = 0,
    'ALTER TABLE user_interface_info ADD UNIQUE KEY uk_user_interface_info_user_interface (userId, interfaceInfoId, isDelete)',
    'SELECT ''uk_user_interface_info_user_interface already exists'''
);

PREPARE stmt FROM @create_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
