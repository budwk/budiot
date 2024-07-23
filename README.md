# BudIot 开源物联网设备平台

<div align="center">

**[官网](https://budiot.com)** |
**[演示地址](https://demo.budiot.com)** |
**[捐赠清单](https://budwk.com/donation)** |

</div>

## 关于BudIoT
BUDIOT 是一个开源的、企业级的物联网平台，它集成了设备管理、协议解析、消息订阅、场景联动等一系列物联网核心能力，支持以平台适配设备的方式连接海量设备，支持在线下发指令实现远程控制，支持扩展水电气等各类计费业务场景。

本平台是在千万级设备实时计费物联网平台经验基础上，在不损失性能的前提下进行功能删减、结构优化而来，小而美，同时又具备灵活的扩展性。

在线演示地址: [https://demo.budiot.com](https://demo.budiot.com) 用户名: `superadmin` 密码: `1`

## 设备接入

支持多协议（MQTT、HTTP、UDP、TCP）自定义设备协议解析接入。支持 AEP、OneNET、厂家平台等平台对接接入。能满足物联网平台中各类接入场景要求，缩短物联网设备接入研发周期。

## 数据存储

* 设备有效数据：采用 MongoDB 时序集合存储，平台支持 TDengine、ClickHouse、ElasticSearch 等时序数据存储方式的平替
* 设备原始报文：采用 MongoDB 存储，可配置TTL，过期自动删除UP过程数据，减少磁盘占用，降本增效
* 设备事件数据：采用 MongoDB 存储，按年月分表
* 设备指令数据：采用 MongoDB 存储，按年月分表
* 业务数据：支持 MySQL、MariaDB 数据库，可改造为达梦等各类国产数据库
* 缓存数据：采用 Redis 分布式缓存，使用 Jedis、Redisson 客户端连接

## 开发框架

基于自研 BudWk 开源Java微服务框架(单应用版本)，详情请访问 [https://budwk.com](https://budwk.com)

## 开发环境

*   OpenJDK 11 
*   Redis 6.x 
*   MariaDB 10.x 
*   MongoDB 7.0.x
*   RocketMQ 5.2.x

## 许可版本

产品开源免费，同时支持付费订制开发。

*  微信号/QQ号：wizzer 