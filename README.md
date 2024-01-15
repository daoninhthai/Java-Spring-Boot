# Spring Cloud Gateway Microservices

A production-ready microservices architecture built with Spring Cloud Gateway, Eureka Service Discovery, and supporting services.

## Architecture

```
                    +-------------------+
                    |   API Gateway     |
                    | (Spring Cloud GW) |
                    |     :8080         |
                    +---------+---------+
                              |
              +---------------+---------------+
              |               |               |
    +---------+---+  +--------+----+  +-------+------+
    | User Service|  |Order Service|  |Product Service|
    |    :8081    |  |    :8082    |  |    :8083      |
    +------+------+  +------+------+  +--------------+
           |                |
    +------+------+  +------+------+
    |    MySQL    |  |    MySQL    |
    |    :3306    |  |    :3307    |
    +-------------+  +-------------+

    +-------------+  +-------------+
    |   Eureka    |  |    Redis    |
    |    :8761    |  |    :6379    |
    +-------------+  +-------------+
```

## Tech Stack

- **Java 17** with **Spring Boot 2.7.8**
- **Spring Cloud Gateway** - API Gateway with routing, filtering, and rate limiting
- **Spring Cloud Netflix Eureka** - Service Discovery
- **Spring Security + JWT** - Authentication and authorization
- **MySQL 8.0** - Primary database
- **Redis 7** - Rate limiting and caching
- **Resilience4j** - Circuit breaker pattern
- **Docker & Docker Compose** - Containerization

## Features

- Dynamic routing with Eureka service discovery
- JWT-based authentication filter
- Request/response logging with correlation IDs
- Rate limiting using Redis
- Circuit breaker pattern with Resilience4j
- Health checks and actuator endpoints
- Multi-stage Docker builds

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### Run with Docker Compose

```bash
docker-compose up -d
```

### Run Locally

1. Start MySQL and Redis:
```bash
docker-compose up -d mysql redis
```

2. Start Discovery Service:
```bash
cd discovery-service && mvn spring-boot:run
```

3. Start Gateway Service:
```bash
cd gateway-service && mvn spring-boot:run
```

4. Start User Service:
```bash
cd user-service && mvn spring-boot:run
```

### API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/users/register | Register new user |
| POST | /api/users/login | User login |
| GET | /api/users | List all users |
| GET | /api/users/{id} | Get user by ID |
| PUT | /api/users/{id} | Update user |
| DELETE | /api/users/{id} | Delete user |

### Eureka Dashboard

Access at: http://localhost:8761 (credentials: eureka/eureka123)

## Configuration

Key configurations are managed via `application.yml` files in each service.

## License

MIT License
