package com.sensorhub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "sensor_realtime_channel")
@Data
public class SensorRealtimeChannel implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String channelCode;

    private String name;

    private String wsPath;

    private String topic;

    private String deviceType;

    private String authType;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
