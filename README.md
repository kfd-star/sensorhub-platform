# SensorHub Platform

SensorHub Platform 是一个面向输电线路监测场景的全栈传感器数据接入与开放接口平台，统一整合了设备注册、接口发布、网关鉴权、实时监控与历史数据查询能力。

## 项目亮点

- 提供统一的开放接口发布流程，支持接口上线、下线、调用统计和访问控制
- 支持传感器设备注册、数据源管理、接口绑定与北向能力映射
- 基于 AK / SK 的网关集中鉴权，统一处理签名校验、请求校验和调用计数
- 支持基于 WebSocket 的实时监控与多源传感数据历史查询
- 基于 Spring Boot Starter 封装 Java 客户端 SDK，简化 GET / POST 接口调用
- 采用多模块后端架构，拆分平台管理、网关、接口服务、公共模块与 SDK

## 技术栈

- 后端：Spring Boot、MyBatis-Plus、MySQL、Redis、Dubbo、Spring Cloud Gateway、WebSocket
- 前端：React、Umi、Ant Design Pro、TypeScript
- 工具链：Maven 多模块、OpenAPI / Knife4j、npm

## 仓库结构

```text
sensorhub-platform/
|-- sensorhub-backend/
|   |-- sensorhub-backend/        # 平台管理后端
|   |-- sensorhub-common/         # 公共模型与 Dubbo 契约
|   |-- sensorhub-client-sdk/     # Java 客户端 SDK
|   |-- sensorhub-gateway/        # API 网关与签名校验
|   `-- sensorhub-interface/      # 接口代理服务
|-- sensorhub-frontend/           # 门户与后台前端
`-- scripts/                      # 本地启动与迁移脚本
```

## 核心能力

### 开放接口平台

- 支持北向接口的发布、下线与管理
- 支持接口调用身份追踪与调用次数统计
- 支持 GET 和 POST 两类接口调用方式

### 传感器工作台

- 支持设备、数据源、实时通道、接口绑定等统一管理
- 支持多类传感器历史数据查询
- 支持在平台页面中查看实时数据更新

### 安全与集成

- 支持基于 AK / SK 的请求签名认证
- 支持在网关侧统一处理请求校验与路由转发
- 提供 Java SDK 以简化外部系统接入

## 快速开始

### 环境要求

- JDK 17
- Node.js 18
- MySQL 8+
- Redis
- Nacos

### 环境变量

启动后端前，请先在本地环境中配置以下变量：

```bash
MYSQL_URL=jdbc:mysql://127.0.0.1:3306/sensorhub
MYSQL_USERNAME=root
MYSQL_PASSWORD=your-password
REDIS_HOST=localhost
REDIS_PORT=6379
NACOS_ADDRESS=nacos://localhost:8848
PLATFORM_PUBLIC_BASE_URL=http://localhost:7529/api
```

### 启动顺序

1. 启动 MySQL、Redis 和 Nacos
2. 导入 `sensorhub-backend/sql/` 下的数据库脚本
3. 启动 `sensorhub-interface`
4. 启动 `sensorhub-gateway`
5. 启动 `sensorhub-backend`
6. 启动 `sensorhub-frontend`

## 默认端口

- 后端管理服务：`7529`
- API 网关：`8090`
- 接口代理服务：`8123`
- 前端：`18000`

## 说明

- 配置文件中的本地敏感信息已替换为公开仓库可用的占位形式
- 构建产物、运行日志和本地依赖目录均已排除出版本控制
