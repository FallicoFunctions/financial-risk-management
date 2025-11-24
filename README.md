# Financial Risk Management Platform

A production-ready, real-time fraud detection and risk management platform built with Spring Boot 3.2, demonstrating enterprise-grade architecture patterns, machine learning integration, and event-driven microservices design.

---

## Interview Discussion Points

This section highlights key architectural decisions and trade-offs for technical discussion.

### Why These Technology Choices?

| Decision | Why | Alternative Considered |
|----------|-----|------------------------|
| **Spring WebFlux (Reactive)** | Non-blocking I/O handles high transaction throughput with fewer threads. A single instance can handle thousands of concurrent connections without thread-per-request overhead. | Spring MVC - simpler but blocks threads waiting for DB/Kafka responses |
| **R2DBC over JPA** | Fully reactive database access. Traditional JDBC blocks threads during queries, negating WebFlux benefits. | JPA/Hibernate - more mature but blocking; would create backpressure bottlenecks |
| **Kafka over RabbitMQ** | Kafka provides durable event log with replay capability (critical for audit trails), better horizontal scaling, and exactly-once semantics for financial data. | RabbitMQ - simpler setup but lacks native event replay and ordering guarantees |
| **Ensemble ML (Rules + Model)** | Rules provide explainability and instant updates (no retraining); ML catches patterns rules miss. Regulators require explainable decisions. | Pure ML - higher accuracy but "black box" decisions violate GDPR Article 22 |
| **Event Sourcing** | Financial systems require complete audit trails. Can replay events to reconstruct state at any point in time for compliance/forensics. | CRUD with audit columns - simpler but loses event history and "why" context |

### Key Design Decisions Worth Discussing

**1. Fire-and-Forget Pattern for Fraud Detection**
```
API Response Time: ~50ms (save transaction + publish event)
Fraud Detection: Runs async after response
```
*Trade-off*: User gets fast response, but fraud decision happens after. Acceptable because blocking would cause 200ms+ latency, and most transactions are legitimate.

**2. Why Not Pure Microservices?**

This is a modular monolith by design. For a fraud detection system:
- **Latency matters**: Inter-service calls add 10-50ms each
- **Consistency matters**: Distributed transactions across services are complex
- **Deployment simplicity**: Single deployment unit is easier to reason about

*When to split*: If different teams owned fraud-rules vs. ML-scoring, or if scaling requirements diverged significantly.

**3. Immutable User Risk Profiles**

Risk profiles are computed from transaction history, not mutated incrementally:
```java
// Recompute from source of truth
profile = computeFromTransactionHistory(userId);
// NOT: profile.incrementRiskScore(0.1);
```
*Why*: Prevents drift, enables replay, ensures consistency. Trade-off is slightly higher compute cost.

**4. WebSocket vs. Server-Sent Events (SSE)**

Chose WebSocket for bidirectional capability (future: analyst can send commands back). SSE would be simpler for pure server-push but limits future extensibility.

### Production Considerations

**Scaling Strategy**
- Stateless application servers (horizontal scaling)
- Kafka partitioned by userId (ensures ordering per user)
- Redis cluster for distributed caching
- PostgreSQL read replicas for query load

**Failure Handling**
- Kafka consumer retries with exponential backoff
- Dead letter queues for poison messages
- Circuit breakers on external service calls (alerts, notifications)
- Graceful degradation: if Redis down, compute profiles on-demand

**What I'd Add for True Production**
- Rate limiting on API endpoints
- Request tracing (Jaeger/Zipkin integration)
- ML model versioning and A/B testing
- Canary deployments for rule changes
- PII encryption at rest

### Questions I Can Discuss In-Depth

1. **"Walk me through a transaction's lifecycle"** - From API call through Kafka to fraud decision
2. **"How would you scale this to 10x traffic?"** - Partitioning strategies, caching layers
3. **"What happens if Kafka goes down?"** - Resilience patterns, circuit breakers
4. **"How do you prevent false positives from blocking legitimate users?"** - Threshold tuning, feedback loops, model monitoring
5. **"How would you debug a production fraud detection issue?"** - Event replay, audit logs, distributed tracing

---

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

## Path to Production

This section documents what would be required to deploy this system to a production environment. This transparency demonstrates understanding of the gap between a demonstration project and production-ready software.

### Current State: What's Proven

| Aspect | Status | Evidence |
|--------|--------|----------|
| Business Logic | Verified | 164 tests covering fraud rules, scoring, workflows |
| API Contracts | Verified | Controller tests validate request/response shapes |
| Kafka Integration | Verified | Embedded Kafka integration tests |
| Database Schema | Verified | R2DBC repository tests with real PostgreSQL |
| WebSocket Streaming | Verified | Handler tests with mock WebSocket sessions |
| ML Explainability | Verified | Feature contribution and explanation tests |

### Current State: What's Not Proven

| Aspect | Status | Why |
|--------|--------|-----|
| Production Load | Untested | No load testing performed |
| Real Notifications | Mock Only | Slack/Email/SMS use mock implementations |
| Failure Recovery | Designed, Not Tested | Retry patterns exist but no chaos testing |
| Geographic Distribution | Not Designed | Single-region deployment assumed |
| Real ML Model | Simulated | Uses rule-based scoring, not trained ML model |

### Production Readiness Checklist

#### Infrastructure (Estimated: 2-3 weeks)

- [ ] **Kubernetes Deployment**
  - Helm charts for application, PostgreSQL, Redis, Kafka
  - Horizontal Pod Autoscaler configuration
  - Resource limits and requests tuned via load testing
  - Liveness/readiness probes (already implemented at `/api/v1/health/*`)

- [ ] **Database**
  - PostgreSQL RDS/Cloud SQL with read replicas
  - Connection pooling (PgBouncer or similar)
  - Automated backups and point-in-time recovery
  - Database migration CI/CD (Liquibase already in place)

- [ ] **Kafka**
  - Managed Kafka (Confluent Cloud, AWS MSK, or similar)
  - Topic partitioning strategy (by userId for ordering)
  - Schema Registry for event schema evolution
  - Dead letter queue topics for failed messages

- [ ] **Redis**
  - Redis Cluster or Elasticache for HA
  - Sentinel for automatic failover
  - Eviction policies configured

#### Security (Estimated: 1-2 weeks)

- [ ] **Authentication/Authorization**
  - OAuth2/OIDC integration (Keycloak, Auth0, Okta)
  - JWT validation on API endpoints
  - Role-based access control (ADMIN, ANALYST, API_USER)
  - API key management for external integrations

- [ ] **Data Protection**
  - PII encryption at rest (user IDs, IP addresses)
  - TLS 1.3 for all connections
  - Secrets management (Vault, AWS Secrets Manager)
  - Database column-level encryption for sensitive fields

- [ ] **Security Scanning**
  - OWASP dependency check in CI/CD
  - Container image scanning (Trivy, Snyk)
  - Penetration testing before go-live

#### Observability (Estimated: 1 week)

- [ ] **Distributed Tracing**
  - OpenTelemetry instrumentation
  - Jaeger or Zipkin for trace visualization
  - Trace context propagation through Kafka

- [ ] **Logging**
  - Structured JSON logging (already using Slf4j)
  - Centralized log aggregation (ELK, Datadog, Splunk)
  - Correlation IDs across services

- [ ] **Alerting**
  - PagerDuty/OpsGenie integration for critical alerts
  - Alert thresholds: error rates, latency p99, Kafka lag
  - Runbooks for common incidents

- [ ] **Dashboards**
  - Grafana dashboards for Prometheus metrics
  - Business metrics: transactions/sec, fraud rate, block rate
  - ML model performance dashboard (AUC-ROC trends)

#### Real Service Integrations (Estimated: 1-2 weeks)

- [ ] **Notification Services** (currently mocked)
  ```java
  // Replace mock implementations with:
  - SendGrid or AWS SES for email
  - Twilio for SMS
  - Slack API for team alerts
  - PagerDuty for critical escalations
  ```

- [ ] **External Data Enrichment**
  - IP geolocation service (MaxMind, IP2Location)
  - Device fingerprinting
  - Merchant category code validation

#### ML Model Production (Estimated: 2-4 weeks)

- [ ] **Model Training Pipeline**
  - Historical transaction data for training
  - Feature store for consistent feature engineering
  - Model versioning (MLflow, SageMaker)
  - A/B testing framework for model comparison

- [ ] **Model Serving**
  - Replace simulated model with trained model
  - Model inference latency < 10ms requirement
  - Fallback to rules-only if model unavailable

- [ ] **Continuous Learning**
  - Feedback loop from analyst reviews
  - Model retraining pipeline (weekly/monthly)
  - Model drift detection and alerting

#### CI/CD Pipeline (Estimated: 1 week)

- [ ] **Build Pipeline**
  ```yaml
  stages:
    - test (unit, integration)
    - security-scan (OWASP, Snyk)
    - build (Docker image)
    - deploy-staging
    - integration-tests-staging
    - deploy-production (manual gate)
  ```

- [ ] **Deployment Strategy**
  - Blue/green or canary deployments
  - Automated rollback on error rate spike
  - Database migration safety checks

#### Load Testing (Estimated: 1 week)

- [ ] **Performance Baselines**
  - Target: 1,000 transactions/second per instance
  - p99 latency < 100ms for transaction creation
  - Kafka consumer lag < 1,000 messages

- [ ] **Load Test Scenarios**
  - Sustained load (1 hour at target TPS)
  - Spike test (10x traffic for 5 minutes)
  - Soak test (24 hours at 50% capacity)

### Effort Estimate Summary

| Category | Effort | Dependencies |
|----------|--------|--------------|
| Infrastructure | 2-3 weeks | DevOps/Platform team |
| Security | 1-2 weeks | Security review |
| Observability | 1 week | Monitoring stack selection |
| Service Integrations | 1-2 weeks | Vendor accounts, API keys |
| ML Production | 2-4 weeks | Data science team, training data |
| CI/CD | 1 week | CI/CD platform selection |
| Load Testing | 1 week | Infrastructure provisioned |
| **Total** | **9-15 weeks** | Parallelizable to ~6-8 weeks |

### Why This Section Exists

This documentation demonstrates:

1. **Self-awareness** - Understanding the difference between demo and production code
2. **Production thinking** - Knowing what "production-ready" actually means
3. **Estimation ability** - Providing realistic effort estimates
4. **Risk identification** - Calling out what's untested and why it matters

> "A senior engineer knows what they don't know. This project proves I can build the architecture; this section proves I understand what comes next."

---

## License

This project is for demonstration purposes.

## Author

Nick Fallico

---

*Built with Spring Boot 3.2.1, demonstrating enterprise-grade fraud detection and risk management patterns.*
