package com.sensorhub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "sensor_interface_binding")
@Data
public class SensorInterfaceBinding implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sensorApiEndpointId;

    private Long interfaceInfoId;

    private String publishStrategy;

    private String authStrategy;

    private String limitStrategy;

    private String cacheStrategy;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
