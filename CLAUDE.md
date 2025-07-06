# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Run
```bash
# Run with H2 database (development)
./gradlew bootRun

# Run with Docker Compose (PostgreSQL)
docker-compose up -d

# Build JAR
./gradlew build

# Clean build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Docker Operations
```bash
# Build Docker image
docker build -t ttb-order-service .

# Push to Docker Hub (requires updating USERNAME in script)
chmod +x build-and-push.sh
./build-and-push.sh v1.0.0
```

## Architecture Overview

### Core Structure
This is a Spring Boot microservice for food delivery order management with:
- **Domain-Driven Design**: Rich domain models with business logic encapsulated in entities
- **Layered Architecture**: Controller → Service → Repository → Domain
- **Microservices Communication**: REST clients for external service integration
- **Event-Driven Integration**: Webhook endpoints for external service callbacks

### Key Components
- **Order Aggregate**: `Order` entity with `OrderItem` value objects, manages order lifecycle
- **State Machine**: `OrderStatus` enum with transition validation (WAITING_FOR_CONFIRMATION → CONFIRMED → COOKING → READY_FOR_DELIVERY → DELIVERING → COMPLETED)
- **External Services**: Payment, Notification, and Inventory service integration via `ExternalServiceClient`
- **Repository Pattern**: JPA-based data access with custom queries for performance

### Package Structure
```
com.ttbspark.fooddelivery.order/
├── controller/          # REST API endpoints and exception handling
├── service/            # Business logic and external service orchestration
├── domain/             # Core business entities and value objects
├── repository/         # Data access layer with JPA repositories
├── dto/               # API request/response contracts
└── exception/         # Custom business exceptions
```

### Database Design
- **Multi-profile support**: H2 (development), PostgreSQL (production/docker)
- **JPA/Hibernate**: Entity relationships with lazy loading optimization
- **Connection pooling**: Built-in HikariCP for production performance
- **Migration strategy**: DDL auto-generation for dev, validate for production

### External Service Integration
- **Async communication**: WebClient for non-blocking HTTP calls
- **Resilience patterns**: Timeout configuration, error handling without cascading failures
- **Service discovery**: Configuration-based service URLs with environment variable support
- **Webhook support**: Dedicated endpoints for external service status callbacks

### Configuration Profiles
- **default**: H2 database, development logging, H2 console enabled
- **docker**: PostgreSQL, container-optimized settings, service discovery
- **production**: PostgreSQL, minimal logging, security hardened

## Testing Strategy

### Test Structure
- **Unit Tests**: Service layer business logic (`OrderServiceTest`)
- **Integration Tests**: Controller layer with MockMvc (`OrderControllerTest`)
- **Mock Framework**: MockK for Kotlin-native mocking
- **Test Data**: In-memory H2 database for isolated testing

### Key Test Patterns
- Status transition validation testing
- External service failure simulation
- Order creation with various payment methods
- Pagination and query testing

## API Design Principles

### RESTful Endpoints
- Standard HTTP methods and response codes
- Resource-based URLs (`/api/orders/{orderNumber}`)
- Consistent error response format via `GlobalExceptionHandler`

### Key Endpoints
- `POST /api/orders` - Order creation
- `GET /api/orders/{orderNumber}` - Order retrieval
- `PUT /api/orders/{orderNumber}/status` - Status updates
- `POST /api/webhooks/*` - External service callbacks

### Validation Strategy
- Bean Validation with JSR-303 annotations
- Custom business rule validation in domain layer
- Global exception handling with structured error responses