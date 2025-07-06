# TTB Spark Food Delivery Platform - Order Service

A robust microservice for handling food delivery orders, built with Kotlin and Spring Boot.

## 🎯 Overview

This is the Order Service for the TTB Spark Food Delivery Platform, designed to handle:
- Order creation and processing
- Order status management (waiting → confirmed → cooking → delivering → completed)
- Payment integration (Bank Transfer & Credit Card)
- Inter-service communication with Payment, Notification, and Inventory services
- Support for 10,000+ transactions per day

## 🏗️ Architecture

### Core Components
- **Order Management**: Complete order lifecycle management
- **Status Transitions**: State machine for order status changes
- **Payment Integration**: Cashless payment processing
- **External Service Integration**: REST clients for microservice communication
- **Webhook Support**: Endpoints for external service callbacks

### Technology Stack
- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.3
- **Database**: PostgreSQL (production) / H2 (development)
- **Build Tool**: Gradle 8.14.2
- **Java Version**: OpenJDK 21
- **Testing**: JUnit 5, MockK, Spring Boot Test

## 🚀 Quick Start

### Prerequisites
- JDK 21+
- Docker & Docker Compose
- Gradle 8.14+ (or use wrapper)

### Running Locally

1. **Clone and Navigate**
   ```bash
   cd order-service
   ```

2. **Run with H2 Database (Development)**
   ```bash
   ./gradlew bootRun
   ```

3. **Run with Docker Compose (Full Stack)**
   ```bash
   docker-compose up -d
   ```

4. **Access Services**
   - Order Service: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console
   - Health Check: http://localhost:8080/actuator/health

## 📋 API Endpoints

### Order Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create new order |
| GET | `/api/orders/{orderNumber}` | Get order details |
| GET | `/api/orders/customer/{customerId}` | Get customer orders |
| GET | `/api/orders/merchant/{merchantId}` | Get merchant orders |
| PUT | `/api/orders/{orderNumber}/status` | Update order status |
| PUT | `/api/orders/{orderNumber}/cancel` | Cancel order |

### Webhooks (Inter-service Communication)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/webhooks/payment-status` | Payment status updates |
| POST | `/api/webhooks/delivery-status` | Delivery status updates |

### Monitoring
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/actuator/metrics` | Application metrics |
| GET | `/api/orders/stats/daily-count` | Daily order statistics |

## 📝 Example Usage

### Create Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "merchantId": 100,
    "paymentMethod": "CREDIT_CARD",
    "deliveryAddress": "123 Sukhumvit Rd, Bangkok 10110",
    "specialInstructions": "Please call when arrived",
    "items": [
      {
        "menuItemId": 1,
        "menuItemName": "Pad Thai",
        "quantity": 2,
        "unitPrice": 120.00,
        "notes": "Extra spicy"
      }
    ]
  }'
```

### Update Order Status
```bash
curl -X PUT http://localhost:8080/api/orders/TTB-123456789-ABC12345/status \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "CONFIRMED",
    "reason": "Order confirmed by merchant"
  }'
```

## 🔄 Order Status Flow

```
WAITING_FOR_CONFIRMATION → CONFIRMED → COOKING → READY_FOR_DELIVERY → DELIVERING → COMPLETED
                ↓             ↓          ↓              ↓              ↓
               CANCELLED ← CANCELLED ← CANCELLED ← CANCELLED ← CANCELLED
```

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Integration Tests
```bash
./gradlew integrationTest
```

### Test Coverage Report
```bash
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## 🐳 Docker Deployment

### Build Image
```bash
./gradlew clean build
docker build -t ttb-order-service .
```

### Run Container
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  ttb-order-service
```

### Push to Docker Hub
```bash
# Update DOCKER_HUB_USERNAME in build-and-push.sh
chmod +x build-and-push.sh
./build-and-push.sh v1.0.0
```

## ⚙️ Configuration

### Environment Variables
| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `default` |
| `DATABASE_URL` | Database connection URL | H2 in-memory |
| `DATABASE_USERNAME` | Database username | `sa` |
| `DATABASE_PASSWORD` | Database password | (empty) |
| `PAYMENT_SERVICE_URL` | Payment service endpoint | `http://payment-service:8081` |
| `NOTIFICATION_SERVICE_URL` | Notification service endpoint | `http://notification-service:8082` |
| `INVENTORY_SERVICE_URL` | Inventory service endpoint | `http://inventory-service:8083` |

### Application Profiles
- **default**: H2 database, development settings
- **docker**: PostgreSQL, container-optimized settings
- **production**: Production-ready configuration

## 🏛️ Microservices Architecture Design

### Recommended Services
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Customer MS   │    │   Merchant MS   │    │  Menu Item MS   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────┬───────────┴───────────────────────┘
                     │
            ┌─────────────────┐
            │   Order MS      │ ← This Service
            │  (This Service) │
            └─────────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   Payment MS    │ │ Notification MS │ │  Inventory MS   │
└─────────────────┘ └─────────────────┘ └─────────────────┘
         │           │           │
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  Banking APIs   │ │   SMS/Email     │ │  Stock Management│
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

### Cloud Services Recommendation (AWS)
- **API Gateway**: Route external requests
- **ECS/EKS**: Container orchestration
- **RDS PostgreSQL**: Managed database
- **ElastiCache Redis**: Caching layer
- **SQS**: Message queuing
- **SNS**: Push notifications
- **CloudWatch**: Monitoring and logging
- **Cognito**: Authentication & authorization
- **ALB**: Load balancing
- **Route 53**: DNS management

## 🔒 Security Features

- Input validation with Bean Validation
- SQL injection prevention with JPA
- Error handling without information leakage
- Health check endpoints for monitoring
- Non-root container execution

## 📊 Performance Considerations

- Connection pooling for database
- Async processing for external service calls
- Pagination for large data sets
- Circuit breaker pattern for external services
- Designed to handle 10,000+ orders/day
