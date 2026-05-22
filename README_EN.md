# BudIot IoT Access Platform

<div align="center">

**[Official Website](https://budiot.com)** |
**[Demo](https://demo.budiot.com)** |
**[中文说明](./README.md)**

</div>

BudIot is a microservice-based IoT platform built on **Spring Boot 4 + Spring Cloud 2025**, covering platform management, device connectivity, protocol parsing, rule engine, messaging, file services, and multi-end admin consoles.

## Tech Stack

| Category | Technology | Version |
|------|------|------|
| Language | Java | 25 |
| Core Framework | Spring Boot | 4.0.4 |
| Microservices | Spring Cloud | 2025.1.1 |
| Alibaba Stack | Spring Cloud Alibaba | 2025.1.0.0 |
| Gateway | Spring Cloud Gateway | 5.0.1 |
| RPC | Apache Dubbo | 3.3.6 |
| Registry / Config Center | Nacos | 3.2.0-BETA |
| ORM | Nutz Dao | 1.r.74-SNAPSHOT |
| Authentication | Sa-Token | 1.45.0 |
| Cache / Message Flow | Redis | - |
| Relational Database | PostgreSQL / MySQL | - |
| Document Database | MongoDB | - |
| Time-Series Database | TDengine | - |
| Frontend | React 19 + Ant Design 6 | - |

## Repository Structure

```text
budiot
├── wk-gateway                         # API gateway
├── wk-platform                        # Platform management services
├── wk-device                          # IoT core modules
│   ├── wk-device-common              # Device entities, DTOs, enums, protocol models
│   ├── wk-device-message             # Device message models and messaging support
│   ├── wk-device-network             # Core network access capabilities
│   ├── wk-device-gateway             # Device gateway / access layer
│   ├── wk-device-handler             # Raw uplink handling, protocol script execution, parsing and dispatch
│   ├── wk-device-rule                # Rule engine
│   ├── wk-device-database            # Device message archiving and storage adapters
│   ├── wk-device-server              # Device management, products, thing model, commands, query APIs
│   └── wk-device-test                # Device-related tests / examples
├── wk-message                         # Messaging center
├── wk-file                            # File service
├── wk-starter                         # Shared infrastructure starters
├── wk-ant-admin                       # Ant Design Pro admin frontend
└── scripts                            # Deployment and operations scripts
```

## Core Capabilities

### Platform Foundation

- API gateway, unified authentication, RBAC, organization management, menus, dictionaries, and system settings
- Dubbo RPC, Nacos registry/config, Redis cache
- Database initialization, shared starters, audit logging, and scheduled job support

### IoT Capabilities

- Products, devices, thing models, protocol scripts, and device commands
- Raw packets, parsed telemetry, event logs, and command logs
- Rule engine, message linkage, device detail pages, and telemetry display
- Multiple storage backends for device data: **DEFAULT / MongoDB / TDengine**

## IoT Architecture Overview

The current `wk-device` module is split into four layers:

1. **wk-device-gateway**: device connectivity and message ingress
2. **wk-device-handler**: raw uplink handling, protocol script execution, parsing properties and events
3. **wk-device-database**: archives raw/data/event/command logs into the configured storage backend
4. **wk-device-server**: device management, product configuration, query APIs, and admin-facing data services

Main uplink flow:

```text
Device Packet -> gateway -> handler -> MQ topic -> database -> storage backend -> server query/API -> admin frontend
```

## Device Data Archiving

### Archive Categories

- **Raw**: original communication packets
- **Data**: parsed property data
- **Event**: device events
- **Command**: platform-issued commands and their status

### DEFAULT (Relational Database)

- Supports monthly partitioned tables
- `device_raw_log_*_*`, `device_event_log_*_*`, and `device_command_log_*_*` store detailed log records
- `device_data_log_*_*` now stores **one row per uplink**
- Besides common base fields, **each thing-model property maps to one column**
- When the thing model changes, the server synchronizes the `device_data_log` table schema
- Property column names follow **`p_<identifier>`** for easier analytics and SQL processing

### MongoDB

- `wk-device-database` now uses **MongoDB time series collections**
- Collections are created automatically by `category + productKey`
- Time field: `ts`
- Metadata field: `meta`
- Retention is controlled with collection-level `expireAfter`
- Existing normal collections are not converted automatically; migrate or recreate them before switching

### TDengine

- Used for time-series device data archiving
- The current implementation includes in-memory queues plus threshold/time-based flushing
- Suitable for high-frequency device telemetry ingestion

## Current Admin Behavior

- Device Detail -> **Reported Data**: each uplink is displayed as one complete row
- Column headers come from the thing model; cell values are shown as **value + unit**
- Device Detail -> **Communication Packets**: when parsed JSON is empty, only `-` is shown
- Device Detail -> **Event Data**: content is displayed as plain text and remains copyable
- In rule management, **Select User** now follows the same interaction pattern as the message sending module

## Requirements

- JDK 25+
- Maven 3.9+
- Node.js 20+
- Nacos 3.x
- Redis 7.x
- PostgreSQL 16+ or MySQL 9.x
- MongoDB 6.0+ recommended when using time series collections
- TDengine when time-series archiving is enabled

## Quick Start

### 1. Prepare Dependencies

Required services:

- Nacos
- Redis
- PostgreSQL or MySQL

Optional, depending on storage configuration:

- MongoDB
- TDengine

### 2. Compile Backend

```bash
mvn clean compile -DskipTests
```

### 3. Start Core Services

A common startup order:

```bash
# Platform management
cd wk-platform/wk-platform-server
mvn spring-boot:run

# Device management
cd ../../wk-device/wk-device-server
mvn spring-boot:run

# Device handler
cd ../wk-device-handler
mvn spring-boot:run

# Device archive service
cd ../wk-device-database
mvn spring-boot:run

# API gateway
cd ../../wk-gateway
mvn spring-boot:run
```

### 4. Start Frontend

```bash
cd wk-ant-admin
npm install
npm run start:dev
```

### 5. Common URLs

| Service | URL |
|------|------|
| API Gateway | http://127.0.0.1:9900 |
| Platform Service | http://127.0.0.1:9901 |
| Ant Admin | http://127.0.0.1:8000 |

Default admin account: `superadmin`  
Default password: `1`

## Configuration

The project supports Maven profiles:

| Profile | Description | Activation |
|---------|------|---------|
| dev | Local development (default) | `-Pdev` |
| test | Test environment | `-Ptest` |
| prod | Production environment | `-Pprod` |

```bash
mvn clean package -Pprod -DskipTests
```

IoT archive-related configuration is under `wk.device.database-ext.*`. Each log category can choose its own backend:

- `storage.raw`
- `storage.data`
- `storage.event`
- `storage.command`

Used together with:

- `mongo-enabled`
- `mongo-uri`
- `tdengine-enabled`
- `relational.partition-enabled`

## Notes

- This root README only documents repository-level capabilities and current implementation conventions
- Environment-specific settings, Nacos configuration, and deployment details should follow each module's actual configuration files
