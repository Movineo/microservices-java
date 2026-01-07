# Point-to-Point Travel Management System - Core Services

This project contains the core infrastructure services for the Point-to-Point Travel Management System, a microservices-based platform for Kenya's transportation sector.

## Services

### 1. API Gateway
- **Description**: Routes requests, handles authentication, and rate limiting.
- **Technology**: Spring Cloud Gateway
- **Port**: 8080
- **Features**:
  - JWT Authentication
  - Rate Limiting with Redis
  - Request Routing to Microservices

### 2. Discovery Service
- **Description**: Manages service registration and discovery.
- **Technology**: Spring Cloud Netflix Eureka
- **Port**: 8761
- **Features**:
  - Service Registry
  - Health Monitoring

### 3. Config Server
- **Description**: Centralizes configuration for all microservices.
- **Technology**: Spring Cloud Config
- **Port**: 8888
- **Features**:
  - Centralized Configuration
  - Dynamic Configuration Updates

## Getting Started

### Prerequisites
- Java 17
- Maven
- Redis (for API Gateway rate limiting)

### Building the Services
```bash
mvn clean install
```

### Running the Services
Start the services in the following order:
1. Discovery Service
```bash
cd discovery-service
mvn spring-boot:run
```

2. Config Server
```bash
cd config-server
mvn spring-boot:run
```

3. API Gateway
```bash
cd api-gateway
mvn spring-boot:run
```

### Testing
- Eureka Dashboard: http://localhost:8761
- API Gateway: http://localhost:8080
- Config Server: http://localhost:8888
