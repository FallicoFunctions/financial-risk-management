# Financial Risk Management Platform

A production-ready, real-time fraud detection and risk management platform built with Spring Boot 3.2, demonstrating enterprise-grade architecture patterns, machine learning integration, and event-driven microservices design.

## Overview

This platform processes financial transactions in real-time, assessing fraud risk using an ensemble approach that combines rule-based detection with probabilistic ML models. It features complete audit trails, explainable AI decisions, and real-time dashboard streaming via WebSockets.

### Key Features

- **Real-time Fraud Detection** - Sub-second transaction risk assessment
- **Ensemble ML Model** - Combines 11 fraud rules with probabilistic scoring
- **Explainable AI** - SHAP-like feature contributions for regulatory compliance
- **Event Sourcing** - Complete audit trail for forensics and compliance
- **Real-time Dashboards** - WebSocket streaming for live monitoring
- **Reactive Architecture** - Non-blocking I/O throughout the stack

## Technology Stack

| Category | Technology |
|----------|------------|
| Framework | Spring Boot 3.2.1, Spring WebFlux |
| Database | PostgreSQL with R2DBC (reactive) |
| Caching | Redis |
| Messaging | Apache Kafka |
| Real-time | WebSocket (reactive handlers) |
| Monitoring | Micrometer, Prometheus |
| Documentation | OpenAPI/Swagger |
| Build | Maven, Java 17 |

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API Gateway                                     │
│                         (REST + WebSocket)                                  │
└─────────────────┬───────────────────────────────────────┬───────────────────┘
                  │                                       │
                  ▼                                       ▼
┌─────────────────────────────┐         ┌─────────────────────────────────────┐
│   Transaction Processing    │         │        WebSocket Handlers           │
│                             │         │  ┌─────────────────────────────┐    │
│  ┌───────────────────────┐  │         │  │ /ws/fraud-alerts            │    │
│  │ TransactionRiskWorkflow│  │         │  │ /ws/transactions            │    │
│  │                       │  │         │  │ /ws/risk-scores             │    │
│  │ • Save Transaction    │  │         │  │ /ws/metrics                 │    │
│  │ • Fraud Detection     │  │         │  └─────────────────────────────┘    │
│  │ • Risk Scoring        │  │         │                                     │
│  └───────────────────────┘  │         │     DashboardEventPublisher         │
└─────────────┬───────────────┘         └──────────────────┬──────────────────┘
              │                                            │
              ▼                                            │
┌─────────────────────────────────────────────────────────┐│
│                    Apache Kafka                          ││
│  ┌─────────────────┐  ┌─────────────────┐               ││
│  │ transaction.*   │  │ fraud.*         │               ││
│  │ user.*          │  │                 │◄──────────────┘│
│  └─────────────────┘  └─────────────────┘                │
└─────────────┬────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Kafka Consumers                                      │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐           │
│  │TransactionConsumer│  │ FraudConsumer    │  │ RiskConsumer     │           │
│  │                  │  │                  │  │                  │           │
│  │• Event Logging   │  │• Alert Services  │  │• Profile Updates │           │
│  │• Analytics       │  │• Notifications   │  │• Compliance      │           │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘           │
└─────────────────────────────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Data Layer                                           │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐           │
│  │   PostgreSQL     │  │     Redis        │  │   Event Store    │           │
│  │   (R2DBC)        │  │   (Cache)        │  │   (Audit Log)    │           │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘           │
└─────────────────────────────────────────────────────────────────────────────┘
```

## ML Fraud Detection

### Ensemble Architecture

The fraud detection system uses an ensemble approach combining:

1. **Rule-Based Engine** (11 fraud rules)
2. **Probabilistic ML Model** (feature-based scoring)

```java
// Ensemble scoring logic
if (rulesTriggered) {
    score = 0.4 * mlProbability + 0.6 * ruleProbability;
} else {
    score = mlProbability;
}
```

### Fraud Detection Rules

| Rule | Description | Risk Weight |
|------|-------------|-------------|
| ImpossibleTravelRule | Detects physically impossible travel patterns | 0.9 |
| HighAmountRule | Transactions > $10,000 | 0.7 |
| VelocityRule | Multiple transactions in short window | 0.8 |
| GeographicAnomalyRule | Unusual transaction location | 0.6 |
| HighRiskMerchantCategoryRule | Gambling, crypto, adult content | 0.7 |
| AmountSpikeRule | Amount >> user average | 0.6 |
| InternationalWithLowHistoryRule | International tx from new user | 0.5 |
| OddHourActivityRule | Transactions 2-5 AM | 0.4 |
| UnusedMerchantCategoryRule | First-time merchant category | 0.3 |
| UnusualDeviationFromAverageRule | Statistical outlier | 0.5 |
| NewUserOnboardingRule | Higher scrutiny for new accounts | 0.4 |

### Explainable AI

Every fraud decision includes SHAP-like feature contributions:

```json
{
  "finalProbability": 0.85,
  "confidence": 0.92,
  "decisionReason": "Transaction BLOCKED: High fraud probability (85%)",
  "featureContributions": [
    {
      "featureName": "amount_deviation",
      "featureDescription": "Transaction amount deviation from user average",
      "contribution": 0.25,
      "impact": "HIGH_RISK"
    }
  ],
  "ruleExplanations": [
    {
      "ruleId": "HIGH_AMOUNT",
      "description": "Transaction exceeds $10,000 threshold",
      "severity": "HIGH"
    }
  ]
}
```

### Model Performance Monitoring

Real-time tracking of ML model performance:

- **ROC-AUC** - Area under ROC curve
- **AUC-PR** - Precision-Recall curve (for imbalanced data)
- **Confusion Matrix** - TP, TN, FP, FN at configurable thresholds
- **Per-Rule Performance** - Precision for each fraud rule
- **Optimal Threshold** - F1-maximizing threshold recommendation

## API Reference

### Transaction Endpoints

```
POST   /api/transactions                           Create transaction with risk assessment
GET    /api/transactions/user/{userId}             Get user's transactions
GET    /api/transactions/daily-total/{userId}      Get daily transaction total
GET    /api/transactions/{transactionId}           Get transaction with fraud status
GET    /api/transactions/{transactionId}/fraud-events  Get fraud events for transaction
```

### User Risk Endpoints

```
GET    /api/users/{userId}/risk-profile            Get comprehensive risk profile
GET    /api/users/{userId}/transaction-statistics  Get transaction statistics
GET    /api/users/{userId}/risk-level              Get risk level summary
GET    /api/users/{userId}/fraud-history           Get paginated fraud history
```

### Admin Endpoints

```
GET    /api/admin/flagged-transactions             Get transactions for review
PUT    /api/admin/transactions/{id}/review         Submit fraud review decision
GET    /api/admin/fraud-rules                      Get all fraud detection rules
```

### ML Performance Endpoints

```
GET    /api/ml/performance                         Comprehensive model metrics
GET    /api/ml/performance/roc                     ROC curve data
GET    /api/ml/performance/pr                      Precision-Recall curve data
GET    /api/ml/performance/thresholds              Metrics at various thresholds
GET    /api/ml/performance/rules                   Per-rule performance stats
POST   /api/ml/feedback                            Submit prediction feedback
GET    /api/ml/health                              Model health summary
```

### Health Endpoints

```
GET    /api/v1/health                              Basic health check
GET    /api/v1/health/detailed                     Component-level health
GET    /api/v1/health/liveness                     Kubernetes liveness probe
GET    /api/v1/health/readiness                    Kubernetes readiness probe
```

## WebSocket Endpoints

Connect to these endpoints for real-time streaming:

| Endpoint | Description | Message Type |
|----------|-------------|--------------|
| `/ws/fraud-alerts` | Fraud detection alerts | FraudAlertMessage |
| `/ws/transactions` | Transaction events | TransactionEventMessage |
| `/ws/risk-scores` | User risk updates | RiskScoreUpdateMessage |
| `/ws/metrics` | Platform metrics (5s interval) | MetricsSnapshotMessage |

### WebSocket Message Example

```json
{
  "type": "FRAUD_DETECTED",
  "timestamp": "2024-11-24T10:30:00Z",
  "transactionId": "txn-123",
  "userId": "user-456",
  "amount": 15000.00,
  "fraudProbability": 0.87,
  "riskLevel": "CRITICAL",
  "triggeredRules": ["HIGH_AMOUNT", "VELOCITY"],
  "action": "BLOCK"
}
```

## Kafka Event Topics

| Topic | Event Type | Description |
|-------|------------|-------------|
| `transaction.created` | TransactionCreatedEvent | New transaction processed |
| `fraud.detected` | FraudDetectedEvent | Fraud risk identified |
| `fraud.cleared` | FraudClearedEvent | Transaction passed checks |
| `transaction.blocked` | TransactionBlockedEvent | High-risk transaction blocked |
| `user.profile.updated` | UserProfileUpdatedEvent | Risk score changed |
| `user.high-risk` | HighRiskUserIdentifiedEvent | Critical risk alert |

## Database Schema

### Core Tables

**transactions**
- Primary transaction data
- Indexed on (user_id, created_at) and merchant_category

**user_risk_profiles**
- User risk scores and transaction statistics
- Includes behavioral and transaction risk components

**merchant_category_frequency**
- Per-user merchant category usage patterns
- Used for unusual merchant detection

**event_log**
- Complete event sourcing audit trail
- JSONB storage for event data and metadata
- Sequence numbers for strict ordering

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Redis 7+
- Apache Kafka 3.x

### Configuration

```properties
# Database
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/financial_risk_management
spring.r2dbc.username=postgres
spring.r2dbc.password=password

# Redis
spring.redis.host=localhost
spring.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
```

### Running the Application

```bash
# Start dependencies (PostgreSQL, Redis, Kafka)
docker-compose up -d

# Run the application
./mvnw spring-boot:run

# Run tests
./mvnw test
```

### API Documentation

Once running, access Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

## Testing

The project includes comprehensive tests:

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=FraudRuleEngineTest

# Run with coverage
./mvnw test jacoco:report
```

### Test Categories

- **Unit Tests** - Individual component testing
- **Integration Tests** - Kafka consumer/producer tests
- **WebSocket Tests** - Real-time streaming tests
- **ML Tests** - Model performance and explainability tests

## Project Structure

```
src/main/java/com/nickfallico/financialriskmanagement/
├── controller/          # REST API controllers
├── service/             # Business logic
│   ├── alert/          # Alert implementations
│   ├── notification/   # Notification channels
│   └── analytics/      # ML/BI services
├── model/              # Domain entities
├── repository/         # R2DBC repositories
├── kafka/              # Event streaming
│   ├── producer/       # Event publishers
│   ├── consumer/       # Event handlers
│   └── event/          # Event DTOs
├── ml/                 # Machine learning
│   ├── rules/          # Fraud detection rules
│   └── explainability/ # SHAP-like explanations
├── websocket/          # Real-time streaming
│   ├── handler/        # WebSocket handlers
│   └── message/        # Message DTOs
├── eventstore/         # Event sourcing
└── config/             # Spring configurations
```

## Architectural Patterns

| Pattern | Implementation |
|---------|----------------|
| Event Sourcing | Complete audit trail in event_log table |
| CQRS | Event log as source of truth, projections for queries |
| Fire-and-Forget | Async fraud detection after API response |
| Reactive Streams | WebFlux, R2DBC, Reactor throughout |
| Cache-Aside | Redis caching with manual invalidation |
| Ensemble ML | Rule engine + probabilistic model |

## Compliance Features

- **GDPR Right to Explanation** - Explainable AI for every fraud decision
- **Complete Audit Trail** - Event sourcing with sequence numbers
- **Metadata Tracking** - Kafka metadata stored (topic, partition, offset)
- **Risk Score History** - All score changes with reasons
- **Forensic Capabilities** - Event replay for investigation

## Monitoring

### Prometheus Metrics

Key metrics exposed at `/actuator/prometheus`:

```
ml.model.auc_roc          # Model ROC-AUC score
ml.model.precision        # Precision at 0.5 threshold
ml.model.recall           # Recall at 0.5 threshold
ml.model.f1_score         # F1 score at optimal threshold
websocket.connections     # Active WebSocket connections
kafka.consumer.lag        # Consumer lag per partition
```

### Health Checks

```json
{
  "status": "UP",
  "components": {
    "database": "UP",
    "redis": "UP",
    "kafka": "UP",
    "fraudModel": {
      "status": "EXCELLENT",
      "aucRoc": 0.92
    }
  }
}
```

## License

This project is for demonstration purposes.

## Author

Nick Fallico

---

*Built with Spring Boot 3.2.1, demonstrating enterprise-grade fraud detection and risk management patterns.*
