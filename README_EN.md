# BudIot Open Source IoT Device Platform

<div align="center">

**[Website](https://budiot.com)** |
**[Demo](https://demo.budiot.com)** |
**[中文](./README.md)**

</div>

## About BudIoT
BUDIOT is an open-source, enterprise-grade IoT platform that integrates device management, protocol parsing, message subscription, scenario linkage, and other core IoT capabilities. It supports connecting massive devices through platform adaptation, enables remote control via online command delivery, and supports various metering business scenarios such as water, electricity, and gas.

This platform is derived from a real-time billing IoT platform with tens of millions of devices. It has been optimized in structure and reduced in functionality without sacrificing performance, making it lightweight yet flexible for extension.

Demo: [https://demo.budiot.com](https://demo.budiot.com) Username: `superadmin` Password: `1`

## Device Access

Supports multi-protocol (MQTT, HTTP, UDP, TCP) custom device protocol parsing. Compatible with platforms like AEP, OneNET, and manufacturer platforms. Meets various access requirements in IoT platforms and shortens IoT device access development cycles.

## Data Storage

* Device effective data: Uses MongoDB time series collections, with support for alternative time series storage solutions like TDengine, ClickHouse, and ElasticSearch
* Device raw messages: Stored in MongoDB with configurable TTL for automatic deletion of expired UP process data, reducing disk usage and improving efficiency
* Device event data: Stored in MongoDB, partitioned by year and month
* Device command data: Stored in MongoDB, partitioned by year and month
* Business data: Supports MySQL and MariaDB databases, adaptable to various domestic databases like DM
* Cache data: Uses Redis distributed cache with Jedis and Redisson clients

## Development Framework

Based on the self-developed BudWk open-source Java microservice framework (single application version). For details, visit [https://budwk.com](https://budwk.com)

## Development Environment

* OpenJDK 11
* Redis 6.x or above
* MariaDB 10.x or PostgreSQL 13.x or above
* MongoDB 7.0.x or above
* RocketMQ 5.2.x or above, or RabbitMQ 3.8.x or above

## License

The open-source version of this project (Budiot) is limited to personal or self-use projects. Without the author's authorization, selling the source code or using it for commercial projects is prohibited.

In case of violation, the author reserves the right to pursue legal action.

* Non-paying users QQ group: 24457628
* Paying users contact WeChat/QQ: wizzer (Note: Business Inquiry)

## Service Support

| Service | Content | Fee | Method |
|---------|---------|-----|--------|
| Basic Issues | Q&A | Free | QQ Group `24457628` |
| System Deployment | System deployment | Free | QQ Group `24457628` |
| Product Usage | Feature usage | Free | QQ Group `24457628` |
| Technical Support | Assistance with deployment and feature usage issues | 100 Yuan | Online remote support within 30 minutes |
| Protocol Development | Writing and providing source code for platform protocol packages based on device models | 3000+ Yuan | Customized development |
| Hardware Support | Luat DTU, providing complete access video documentation | N/A | [Luat Official Website](https://official.openluat.com/) |
| Other Services | Commercial authorization, customized development, etc. | Negotiable | Contact WeChat `wizzer` (Note: Business Inquiry) | 