# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Project Overview

IFP SPC (Statistical Process Control) module - Statistical process control for industrial manufacturing, built on Spring Boot with Alibaba Cloud Nacos discovery/config. Supports real-time data analysis via TDengine time-series database.

## Technology Stack

- Java 11+
- Maven build system
- Spring Boot (with parent pom: `px-ifp-platform`)
- MyBatis-Plus for ORM
- MySQL for relational data
- TDengine 3.5.1 for time-series data storage
- Kafka for event streaming
- Redis for caching (Caffeine cache also used)
- Nacos for service discovery/config
- XXL-Job 2.4.0 for scheduled tasks
- OpenFeign for service calls

## Common Commands

### Build
```bash
mvn clean package -DskipTests
```

### Compile without building
```bash
mvn compile
```

### Run tests
```bash
mvn test
```

### Start local infrastructure (Redis, Kafka, ZK, Nacos, XXL-Job, TDengine)
```bash
docker-compose -f docker-compose-dev.yml up -d
```

### Stop local infrastructure
```bash
docker-compose -f docker-compose-dev.yml down
```

### View infrastructure logs
```bash
docker-compose -f docker-compose-dev.yml logs -f
```

## Code Architecture

### Package Structure

```
com.px.ifp.spc/
├── Spc.java                   - Main Spring Boot application entry point
├── bo/                        - Business objects
├── configure/                 - Configuration classes
├── constant/                  - Constants
├── dto/                       - Data transfer objects
│   ├── manager/               - Manager DTOs
│   └── publish/               - Publish DTOs
├── entity/                    - Database entities (MyBatis-Plus)
├── enums/                     - Enum definitions
├── error/                     - Error handling
├── handle/                    - Handlers (exception, data processing)
├── interceptor/               - Interceptors
├── mapper/                    - MyBatis mappers
├── mock/                      - Mock data generation
├── mq/                        - Kafka message consumers/producers
├── properties/                - Configuration properties
├── remote/                    - Feign remote clients
├── service/                   - Business logic services
├── util/                      - Utilities
└── web/                       - REST controllers
    ├── manager/               - Manager/backend API controllers
    └── publish/               - Public/published API controllers
```

### Key Features

1. **SPC Indicator Management** - Configure statistical process control indicators
2. **Sampling Strategy** - Configure data sampling strategies for collection
3. **Control Charts** - Generate and manage SPC control charts
4. **Abnormality Detection (判异)** - Statistical analysis for detecting process abnormalities
5. **Historical Version Management** - Version control for control lines
6. **Tags Feature** - Tagging system for SPC analysis results
7. **Real-time Monitoring** - Real-time anomaly monitoring via TDengine subscriptions
8. **XXL-Job Integration** - Scheduled batch processing jobs

### Data Storage

- **MySQL**: Stores configuration (indicators, sampling strategies, versions, control lines, notes)
- **TDengine**: Stores time-series measurement data for SPC analysis

### Frontend Prototype

- `product/spc_html/` - Axure RP generated HTML prototype of the SPC application UI

## Database

- SQL initialization scripts: `sql/mysql/init_mysql.sql`
- Uses MyBatis-Plus for CRUD operations

## Running the Application

The main class is `com.px.ifp.spc.Spc`. After building, run:

```bash
java -jar target/ifp-spc.jar
```
