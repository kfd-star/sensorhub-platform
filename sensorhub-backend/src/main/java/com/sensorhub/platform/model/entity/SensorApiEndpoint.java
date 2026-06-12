package com.sensorhub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "sensor_api_endpoint")
@Data
public class SensorApiEndpoint implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String endpointCode;

    private String name;

    private String description;

    private String method;

    private String internalPath;

    private String targetService;

    private String targetPath;

    private String category;

    private String protocolType;

    private String requestSchema;

    private String responseSchema;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
