# Kafka Event Schemas Documentation

This document describes all Kafka events published by the Financial Risk Management Platform.

## Event Architecture

The system uses an event-driven architecture with Kafka as the message broker. Events are published asynchronously to enable:
- Real-time fraud detection and alerting
- User risk profile updates
- Analytics and ML model training
- Audit logging and compliance

## Event Topics

| Topic Name | Description | Event Types |
|------------|-------------|-------------|
| `transaction-events` | Transaction lifecycle events | TransactionCreated |
| `fraud-events` | Fraud detection and clearing | FraudDetected, FraudCleared |
| `risk-events` | Risk assessment events | TransactionBlocked, HighRiskUserIdentified |
| `profile-events` | User profile updates | UserProfileUpdated |

## Event Schemas

### 1. TransactionCreatedEvent

Published when a new transaction is created and enters the risk assessment pipeline.

**Topic**: `transaction-events`

**Schema**:
```json
{
  "transactionId": "uuid",
  "userId": "string",
  "amount": "decimal",
  "currency": "string",
  "createdAt": "ISO-8601 timestamp",
  "transactionType": "PURCHASE | WITHDRAWAL | TRANSFER | DEPOSIT",
  "merchantCategory": "string",
  "merchantName": "string",
  "isInternational": "boolean",
  "latitude": "double",
  "longitude": "double",
  "country": "string (ISO 3166-1 alpha-2)",
  "city": "string",
  "ipAddress": "string",
  "eventTimestamp": "ISO-8601 timestamp",
  "eventId": "string",
  "eventSource": "transaction-service"
}
```

**Example**:
```json
{
  "transactionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "userId": "user_12345",
  "amount": 150.00,
  "currency": "USD",
  "createdAt": "2025-11-22T14:30:00Z",
  "transactionType": "PURCHASE",
  "merchantCategory": "RETAIL",
  "merchantName": "Amazon.com",
  "isInternational": false,
  "latitude": 40.7128,
  "longitude": -74.0060,
  "country": "US",
  "city": "New York",
  "ipAddress": "192.168.1.1",
  "eventTimestamp": "2025-11-22T14:30:01Z",
  "eventId": "evt_abc123",
  "eventSource": "transaction-service"
}
```

**Consumer Actions**:
- Fraud detection ML model evaluation
- Transaction pattern analysis
- Geographic anomaly detection
- Velocity checks

---

### 2. FraudDetectedEvent

Published when fraud is detected in a transaction by the ML model or rule-based engine.

**Topic**: `fraud-events`

**Schema**:
```json
{
  "transactionId": "uuid",
  "userId": "string",
  "amount": "decimal",
  "currency": "string",
  "merchantCategory": "string",
  "isInternational": "boolean",
  "fraudProbability": "double (0.0-1.0)",
  "violatedRules": ["string"],
  "riskLevel": "LOW | MEDIUM | HIGH | CRITICAL",
  "action": "REVIEW | BLOCK",
  "eventTimestamp": "ISO-8601 timestamp",
  "eventId": "uuid",
  "eventSource": "fraud-detection-service"
}
```

**Example**:
```json
{
  "transactionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "userId": "user_12345",
  "amount": 15000.00,
  "currency": "USD",
  "merchantCategory": "GAMBLING",
  "isInternational": true,
  "fraudProbability": 0.87,
  "violatedRules": [
    "HIGH_AMOUNT_THRESHOLD",
    "HIGH_RISK_MERCHANT",
    "GEOGRAPHIC_ANOMALY",
    "UNUSUAL_HOUR"
  ],
  "riskLevel": "HIGH",
  "action": "REVIEW",
  "eventTimestamp": "2025-11-22T14:30:05Z",
  "eventId": "9b7d6f58-3b4c-4a5e-b8f1-2c3d4e5f6789",
  "eventSource": "fraud-detection-service"
}
```

**Consumer Actions**:
- Send alerts to fraud investigation team
- Update user risk score
- Trigger notification service for suspicious activity
- Log event for compliance and audit

---

### 3. FraudClearedEvent

Published when a previously flagged transaction is cleared after manual review.

**Topic**: `fraud-events`

**Schema**:
```json
{
  "transactionId": "uuid",
  "userId": "string",
  "clearedBy": "string (reviewer ID)",
  "clearanceReason": "string",
  "reviewNotes": "string",
  "originalFraudProbability": "double",
  "eventTimestamp": "ISO-8601 timestamp",
  "eventId": "uuid",
  "eventSource": "admin-service"
}
```

**Example**:
```json
{
  "transactionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "userId": "user_12345",
  "clearedBy": "admin_jane_doe",
  "clearanceReason": "LEGITIMATE_TRANSACTION",
  "reviewNotes": "Customer verified via phone. Large purchase was for legitimate business expense.",
  "originalFraudProbability": 0.87,
  "eventTimestamp": "2025-11-22T15:45:00Z",
  "eventId": "c1d2e3f4-5a6b-7c8d-9e0f-1a2b3c4d5e6f",
  "eventSource": "admin-service"
}
```

**Consumer Actions**:
- Update user risk profile (reduce risk score)
- Retrain ML model with false positive
- Send notification to user
- Update transaction status to APPROVED

---

### 4. TransactionBlockedEvent

Published when a transaction is automatically blocked due to high fraud risk.

**Topic**: `risk-events`

**Schema**:
```json
{
  "transactionId": "uuid",
  "userId": "string",
  "amount": "decimal",
  "currency": "string",
  "merchantCategory": "string",
  "isInternational": "boolean",
  "blockReason": "string",
  "violatedRules": ["string"],
  "fraudProbability": "double",
  "severity": "MEDIUM | HIGH | CRITICAL",
  "eventTimestamp": "ISO-8601 timestamp",
  "eventId": "uuid",
  "eventSource": "fraud-detection-service"
}
```

**Example**:
```json
{
  "transactionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": "user_67890",
  "amount": 50000.00,
  "currency": "USD",
  "merchantCategory": "CRYPTOCURRENCY",
  "isInternational": true,
  "blockReason": "CRITICAL_FRAUD_THRESHOLD_EXCEEDED",
  "violatedRules": [
    "IMPOSSIBLE_TRAVEL",
    "EXTREME_AMOUNT",
    "HIGH_RISK_MERCHANT",
    "NEW_USER_LARGE_TRANSACTION"
  ],
  "fraudProbability": 0.97,
  "severity": "CRITICAL",
  "eventTimestamp": "2025-11-22T16:00:00Z",
  "eventId": "7f8e9d0c-1b2a-3c4d-5e6f-7890abcdef12",
  "eventSource": "fraud-detection-service"
}
```

**Consumer Actions**:
- Immediate notification to fraud team (high priority)
- Block user account temporarily
- Send SMS/email alert to user
- Create fraud investigation case
- Log to security incident management system

---

### 5. HighRiskUserIdentifiedEvent

Published when a user is identified as high-risk based on behavior patterns.

**Topic**: `risk-events`

**Schema**:
```json
{
  "userId": "string",
  "riskScore": "double (0.0-1.0)",
  "riskLevel": "LOW | MEDIUM | HIGH | CRITICAL",
  "riskFactors": ["string"],
  "totalTransactions": "integer",
  "highRiskTransactionCount": "integer",
  "accountAgeDays": "integer",
  "eventTimestamp": "ISO-8601 timestamp",
  "eventId": "uuid",
  "eventSource": "risk-assessment-service"
}
```

**Example**:
```json
{
  "userId": "user_98765",
  "riskScore": 0.82,
  "riskLevel": "HIGH",
  "riskFactors": [
    "MULTIPLE_HIGH_VALUE_TRANSACTIONS",
    "FREQUENT_INTERNATIONAL_ACTIVITY",
    "UNUSUAL_MERCHANT_CATEGORIES",
    "RAPID_TRANSACTION_VELOCITY"
  ],
  "totalTransactions": 45,
  "highRiskTransactionCount": 12,
  "accountAgeDays": 7,
  "eventTimestamp": "2025-11-22T17:00:00Z",
  "eventId": "9a8b7c6d-5e4f-3210-abcd-ef9876543210",
  "eventSource": "risk-assessment-service"
}
```

**Consumer Actions**:
- Enhanced monitoring for future transactions
- Require additional verification for high-value transactions
- Notify compliance team
- Update user risk profile cache

---

### 6. UserProfileUpdatedEvent

Published when a user's risk profile is recalculated and updated.

**Topic**: `profile-events`

**Schema**:
```json
{
  "userId": "string",
  "previousOverallRiskScore": "double",
  "newOverallRiskScore": "double",
  "totalTransactions": "integer",
  "totalTransactionValue": "double",
  "highRiskTransactions": "integer",
  "updateReason": "string",
  "triggeringTransactionId": "uuid",
  "eventTimestamp": "ISO-8601 timestamp",
  "eventId": "uuid",
  "eventSource": "profile-service"
}
```

**Example**:
```json
{
  "userId": "user_12345",
  "previousOverallRiskScore": 0.45,
  "newOverallRiskScore": 0.62,
  "totalTransactions": 128,
  "totalTransactionValue": 45600.00,
  "highRiskTransactions": 3,
  "updateReason": "FRAUD_DETECTED",
  "triggeringTransactionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "eventTimestamp": "2025-11-22T14:35:00Z",
  "eventId": "1a2b3c4d-5e6f-7890-abcd-ef1234567890",
  "eventSource": "profile-service"
}
```

**Consumer Actions**:
- Invalidate user profile cache
- Update analytics dashboards
- Trigger ML model retraining if significant pattern change
- Feed data to business intelligence systems

---

## Event Consumption Patterns

### Real-time Processing
Events are consumed in real-time for immediate actions:
- Fraud alerts (< 100ms)
- Transaction blocking (< 50ms)
- User notifications (< 1s)

### Batch Processing
Some events are also consumed in batches for:
- Daily ML model retraining
- Weekly fraud pattern analysis
- Monthly compliance reports

### Event Ordering
Events for the same transaction/user are ordered by sequence number in the event store. Consumers should respect event ordering when processing state changes.

### Error Handling
All consumers implement:
- Retry logic with exponential backoff
- Dead letter queues for failed messages
- Idempotency checks (event_id deduplication)
- Circuit breaker patterns for downstream dependencies

### Monitoring
Key metrics tracked for each event type:
- Production rate (events/sec)
- Consumer lag (ms)
- Processing time (ms)
- Error rate (%)
- Dead letter queue depth

## Integration Examples

See [API_USAGE_EXAMPLES.md](./API_USAGE_EXAMPLES.md) for code examples of:
- Publishing events to Kafka
- Consuming events with Spring Kafka
- Event-driven transaction processing flows
