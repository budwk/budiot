# BudIot 物联网接入平台

<div align="center">

**[官网](https://budiot.com)** |
**[演示地址](https://demo.budiot.com)** |
**[English](./README_EN.md)**

</div>

BudIot 是一个基于 **Spring Boot 4 + Spring Cloud 2025** 的微服务物联网平台，覆盖平台管理、设备接入、协议解析、规则引擎、消息通知、文件服务和多端管理后台。

## 技术栈

| 类别 | 技术                             | 版本 |
|------|--------------------------------|------|
| 语言 | Java                           | 25 |
| 核心框架 | Spring Boot                    | 4.0.4 |
| 微服务 | Spring Cloud                   | 2025.1.1 |
| 微服务(阿里) | Spring Cloud Alibaba           | 2025.1.0.0 |
| 网关 | Spring Cloud Gateway           | 5.0.1 |
| RPC | Apache Dubbo                   | 3.3.6 |
| 注册/配置中心 | Nacos                          | 3.2.0-BETA |
| ORM | Nutz Dao                       | 1.r.74-SNAPSHOT |
| 权限认证 | Sa-Token                       | 1.45.0 |
| 缓存/消息流转 | Redis                          | - |
| 关系型数据库 | PostgreSQL / MySQL             | - |
| 文档数据库 | MongoDB                        | - |
| 时序数据库 | TDengine                       | - |
| 前端 | React 19 + Ant Design 6 | - |

## 仓库结构

```text
budiot
├── wk-gateway                         # API 网关
├── wk-platform                        # 平台管理服务
├── wk-device                          # 物联网核心模块
│   ├── wk-device-common              # 设备实体、DTO、枚举、协议模型
│   ├── wk-device-message             # 设备消息模型与消息支持
│   ├── wk-device-network             # 网络接入基础能力
│   ├── wk-device-gateway             # 设备网关/接入层
│   ├── wk-device-handler             # 原始上行处理、协议脚本执行、解析分发
│   ├── wk-device-rule                # 规则引擎
│   ├── wk-device-database            # 设备消息归档与存储适配
│   ├── wk-device-server              # 设备管理、产品、物模型、指令、查询 API
│   └── wk-device-test                # 设备相关测试/示例
├── wk-message                         # 消息中心
├── wk-file                            # 文件服务
├── wk-starter                         # 通用基础设施 Starter
├── wk-ant-admin                       # Ant Design Pro 管理后台
└── scripts                            # 部署与运维脚本
```

## 核心能力

### 平台基础能力

- API 网关、统一认证、RBAC 权限、组织架构、菜单、字典、系统配置
- Dubbo 服务调用、Nacos 注册/配置、Redis 缓存
- 数据库初始化、公共 Starter、日志审计、定时任务支持

### 物联网能力

- 产品、设备、物模型、协议脚本、设备指令
- 原始报文、解析数据、事件、指令日志归档
- 规则引擎、消息联动、设备详情与遥测展示
- 支持多种设备数据存储后端：**DEFAULT / MongoDB / TDengine**

## 物联网架构概览

当前 `wk-device` 按职责拆分为接入、处理、归档、查询四层：

1. **wk-device-gateway**：负责设备接入与消息入口
2. **wk-device-handler**：负责原始上行处理、协议脚本执行、解析 properties / events
3. **wk-device-database**：负责 raw/data/event/command 归档，按配置写入不同存储
4. **wk-device-server**：负责设备管理、产品配置、查询接口和管理端页面数据

设备上行的主链路为：

```text
设备报文 -> gateway -> handler -> MQ topic -> database -> 存储后端 -> server 查询/API -> 管理后台
```

## 设备数据归档说明

### 归档分类

- **Raw**：原始通信报文
- **Data**：协议解析后的属性数据
- **Event**：设备事件
- **Command**：平台下发指令及状态

### DEFAULT（关系型数据库）

- 支持按月分表归档
- `device_raw_log_*_*` / `device_event_log_*_*` / `device_command_log_*_*` 按日志明细存储
- `device_data_log_*_*` 已调整为**一条上报一行**
- `device_data_log_*_*` 中除基础字段外，**每个物模型属性对应一个列**
- 物模型 properties 变更时，服务端会同步调整 `device_data_log` 表结构
- 属性列名规则为 **`p_<identifier>`**，便于后续统计分析

### MongoDB

- `wk-device-database` 现已使用 **MongoDB 时序集合（time series collection）**
- 按 `category + productKey` 自动创建集合
- 时序时间字段：`ts`
- 元数据字段：`meta`
- 保留期通过集合级 `expireAfter` 控制
- 已存在的普通集合不会自动转成时序集合，切换时需先迁移或重建旧集合

### TDengine

- 用于设备时序数据归档
- 当前实现包含内存队列与定时/阈值 flush
- 适合高频设备数据写入场景

## 管理端当前行为

- 设备详情 -> **上报数据**：按一次上报展示一行完整数据
- 表头来自物模型属性定义，单元格展示为 **值 + 单位**
- 设备详情 -> **通信报文**：解析后 JSON 为空时仅显示 `-`
- 设备详情 -> **事件数据**：内容按纯文本展示并支持复制
- 规则管理中的“选择用户”交互已与消息发送模块保持一致

## 环境要求

- JDK 25+
- Maven 3.9+
- Node.js 20+
- Nacos 3.x
- Redis 7.x
- PostgreSQL 16+ 或 MySQL 9.x
- MongoDB 6.0+（启用时序集合时建议使用较新版本）
- TDengine（启用时序归档时）

## 快速开始

### 1. 准备基础依赖

至少准备以下组件：

- Nacos
- Redis
- PostgreSQL 或 MySQL

按需启用：

- MongoDB
- TDengine

### 2. 编译后端

```bash
mvn clean compile -DskipTests
```

### 3. 启动核心服务

常见启动顺序：

```bash
# 平台管理
cd wk-platform/wk-platform-server
mvn spring-boot:run

# 设备管理
cd ../../wk-device/wk-device-server
mvn spring-boot:run

# 设备处理
cd ../wk-device-handler
mvn spring-boot:run

# 设备归档
cd ../wk-device-database
mvn spring-boot:run

# API 网关
cd ../../wk-gateway
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd wk-ant-admin
npm install
npm run start:dev
```

### 5. 常用地址

| 服务 | 地址 |
|------|------|
| API 网关 | http://127.0.0.1:9900 |
| 平台服务 | http://127.0.0.1:9901 |
| Ant 管理端 | http://127.0.0.1:8000 |

默认管理员账号：`superadmin`  密码：`1`

## 配置说明

项目支持 Maven Profile：

| Profile | 说明 | 激活方式 |
|---------|------|---------|
| dev | 本地开发环境（默认） | `-Pdev` |
| test | 测试环境 | `-Ptest` |
| prod | 生产环境 | `-Pprod` |

```bash
mvn clean package -Pprod -DskipTests
```

物联网归档相关配置集中在 `wk.device.database-ext.*`，可按日志类型分别选择存储后端：

- `storage.raw`
- `storage.data`
- `storage.event`
- `storage.command`

并配合以下能力使用：

- `mongo-enabled`
- `mongo-uri`
- `tdengine-enabled`
- `relational.partition-enabled`

## 联系作者

- 用户交流 QQ群：24457628
- 定制开发 微信/QQ：wizzer (备注 budiot)