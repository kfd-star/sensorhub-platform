package com.sensorhub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "sensor_gps_data")
@Data
public class SensorGpsData implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sourceRecordId;

    private String deviceToken;

    private Long timestampMillisecond;

    private Double velocityX;

    private Double velocityY;

    private Double velocityZ;

    private Double rtkVelocityX;

    private Double rtkVelocityY;

    private Double rtkVelocityZ;

    private Double gpsVelocityX;

    private Double gpsVelocityY;

    private Double gpsVelocityZ;

    private Double accelerationGroundX;

    private Double accelerationGroundY;

    private Double accelerationGroundZ;

    private Double accelerationBodyX;

    private Double accelerationBodyY;

    private Double accelerationBodyZ;

    private Double accelerationRawX;

    private Double accelerationRawY;

    private Double accelerationRawZ;

    private Integer rtkYaw;

    private Integer rtkYawInfo;

    private Long gpsDate;

    private Long gpsTime;

    private Integer compassX;

    private Integer compassY;

    private Integer compassZ;

    private Double quaternionQ0;

    private Double quaternionQ1;

    private Double quaternionQ2;

    private Double quaternionQ3;

    private Double pitch;

    private Double roll;

    private Double yaw;

    private Double psi;

    private Double gpsPositionX;

    private Double gpsPositionY;

    private Double gpsPositionZ;

    private Date timestamp;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
