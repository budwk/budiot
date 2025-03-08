# Product Introduction

## About BudIoT
BUDIOT is an open-source, enterprise-grade IoT platform that integrates device management, protocol parsing, message subscription, scenario linkage, and other core IoT capabilities. It supports connecting massive devices through platform adaptation, enables remote control via online command delivery, and supports various metering business scenarios such as water, electricity, and gas.

This platform is derived from a real-time billing IoT platform with tens of millions of devices. It has been optimized in structure and reduced in functionality without sacrificing performance, making it lightweight yet flexible for extension.

Demo: [https://demo.budiot.com](https://demo.budiot.com) Username: `superadmin` Password: `1`

## Device Access

Supports multiple network protocols (MQTT, HTTP, UDP, TCP, Modbus Master, Modbus Slave) for device access with custom protocol parsing. Compatible with platforms like AEP, OneNET, and manufacturer platforms. Meets various access requirements in IoT platforms and shortens IoT device access development cycles.

## Data Storage

* Device effective data: Uses MongoDB time series collections, with support for alternative time series storage solutions like TDengine, ClickHouse, and ElasticSearch
* Device raw messages: Stored in MongoDB with configurable TTL for automatic deletion of expired UP process data, reducing disk usage and improving efficiency
* Device event data: Stored in MongoDB, partitioned by year and month
* Device command data: Stored in MongoDB, partitioned by year and month
* Business data: Supports MySQL and MariaDB databases, adaptable to various domestic databases like DM
* Cache data: Uses Redis distributed cache with Jedis and Redisson clients

## Development Framework

Based on the self-developed BudWk open-source Java microservice framework (single application version). For details, visit [https://budwk.com](https://budwk.com)

## License

The open-source version of this project (Budiot) is limited to personal or self-use projects. Without the author's authorization, selling the source code or using it for commercial projects is prohibited.

In case of violation, the author reserves the right to pursue legal action.

* Non-paying users QQ group: 24457628
* Paying users contact WeChat/QQ: wizzer (Note: Business Inquiry)

| Feature | Community Edition | Enterprise Edition |
| ------- | ----------------- | ------------------ |
| Open Source Code | ✅ | ✅ |
| Device Management, Device Access | ✅ | ✅ |
| Multi-message Protocol Support | ✅ | ✅ |
| MQTT/TCP/UDP/HTTP/MODBUS | ✅ | ✅ |
| OpenAPI | ✅ | ✅ |
| Cluster Support | ✅ | ✅ |
| Microservice Architecture | ✅ | ✅ |
| One-on-one Technical Support | ⭕ | ✅ |
| Custom Development | ⭕ | ✅ (Paid Option) |
| Scenario Services | ⭕ | ✅ (Paid Option) |
| Forward Device Data to RabbitMQ, Kafka | ⭕ | ✅ (Paid Option) |
| TDEngine Storage Support | ⭕ | ✅ (Paid Option) |
| Data Permission Control | ⭕ | ✅ (Paid Option) |
| Alibaba Cloud Protocol Adaptation | ⭕ | ✅ (Paid Option) |
| Alibaba Cloud Platform Access | ⭕ | ✅ (Paid Option) |
| Xiaodu Platform Access | ⭕ | ✅ (Paid Option) |
| China Telecom AEP Platform Access | ⭕ | ✅ (Paid Option) |
| China Mobile OneNet Platform Access | ⭕ | ✅ (Paid Option) |
| Commercial Restrictions | Limited to personal or self-use projects | <span style='color:green;font-weight:800'>Unlimited number of projects</span> |
| Pricing | Free | Contact WeChat `wizzer` (Note: Business Inquiry) |

## Code Architecture

![Code Architecture](/01.png)

## Business Architecture

![Business Architecture](/02.png) 