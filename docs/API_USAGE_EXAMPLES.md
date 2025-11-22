# API Usage Examples

This document provides practical examples of using the Financial Risk Management Platform API.

## Table of Contents
1. [Authentication](#authentication)
2. [Transaction Management](#transaction-management)
3. [Risk Assessment](#risk-assessment)
4. [Fraud Detection](#fraud-detection)
5. [Administrative Operations](#administrative-operations)
6. [Kafka Event Integration](#kafka-event-integration)

---

## Authentication

All API endpoints require HTTP Basic authentication.

### cURL Example
```bash
curl -u username:password https://api.financial-risk.example.com/api/transactions
```

### Java Example
```java
RestTemplate restTemplate = new RestTemplate();
restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor("username", "password"));
```

### Python Example
```python
import requests
from requests.auth import HTTPBasicAuth

auth = HTTPBasicAuth('username', 'password')
response = requests.get('https://api.financial-risk.example.com/api/transactions', auth=auth)
```

---

## Transaction Management

### Create a New Transaction

Creates a new transaction and submits it for real-time fraud detection.

**Endpoint**: `POST /api/transactions`

#### cURL Example
```bash
curl -X POST https://api.financial-risk.example.com/api/transactions \
  -u username:password \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user_12345",
    "amount": 150.00,
    "currency": "USD",
    "transactionType": "PURCHASE",
    "merchantCategory": "RETAIL",
    "merchantName": "Amazon.com",
    "isInternational": false,
    "latitude": 40.7128,
    "longitude": -74.0060,
    "country": "US",
    "city": "New York",
    "ipAddress": "192.168.1.1"
  }'
```

#### Java (WebClient) Example
```java
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

WebClient webClient = WebClient.builder()
    .baseUrl("https://api.financial-risk.example.com")
    .defaultHeaders(headers -> headers.setBasicAuth("username", "password"))
    .build();

TransactionDTO request = TransactionDTO.builder()
    .userId("user_12345")
    .amount(new BigDecimal("150.00"))
    .currency("USD")
    .transactionType(TransactionType.PURCHASE)
    .merchantCategory("RETAIL")
    .merchantName("Amazon.com")
    .isInternational(false)
    .latitude(40.7128)
    .longitude(-74.0060)
    .country("US")
    .city("New York")
    .ipAddress("192.168.1.1")
    .build();

Mono<Transactions> response = webClient.post()
    .uri("/api/transactions")
    .bodyValue(request)
    .retrieve()
    .bodyToMono(Transactions.class);

response.subscribe(transaction -> {
    System.out.println("Transaction created: " + transaction.getId());
    System.out.println("Fraud probability: " + transaction.getFraudProbability());
    System.out.println("Risk level: " + transaction.getRiskLevel());
});
```

#### Python Example
```python
import requests

url = "https://api.financial-risk.example.com/api/transactions"
auth = ("username", "password")
headers = {"Content-Type": "application/json"}

transaction = {
    "userId": "user_12345",
    "amount": 150.00,
    "currency": "USD",
    "transactionType": "PURCHASE",
    "merchantCategory": "RETAIL",
    "merchantName": "Amazon.com",
    "isInternational": False,
    "latitude": 40.7128,
    "longitude": -74.0060,
    "country": "US",
    "city": "New York",
    "ipAddress": "192.168.1.1"
}

response = requests.post(url, json=transaction, auth=auth, headers=headers)

if response.status_code == 200:
    result = response.json()
    print(f"Transaction ID: {result['id']}")
    print(f"Fraud Probability: {result['fraudProbability']}")
else:
    print(f"Error: {response.status_code} - {response.text}")
```

### Get Transaction Status

Retrieves the current status of a transaction including fraud assessment.

**Endpoint**: `GET /api/transactions/{transactionId}`

#### cURL Example
```bash
curl https://api.financial-risk.example.com/api/transactions/f47ac10b-58cc-4372-a567-0e02b2c3d479 \
  -u username:password
```

#### Response Example
```json
{
  "transactionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "userId": "user_12345",
  "amount": 15000.00,
  "currency": "USD",
  "fraudStatus": "FLAGGED",
  "fraudProbability": 0.87,
  "riskLevel": "HIGH",
  "fraudReasons": [
    "HIGH_AMOUNT_THRESHOLD",
    "GEOGRAPHIC_ANOMALY"
  ],
  "processingStatus": "UNDER_REVIEW",
  "violationCount": 2,
  "fraudEvents": [
    {
      "eventType": "FRAUD_DETECTED",
      "eventId": "evt_123",
      "fraudProbability": 0.87,
      "riskLevel": "HIGH",
      "createdAt": "2025-11-22T14:30:05Z"
    }
  ]
}
```

### Get User Transactions

Retrieves all transactions for a specific user.

**Endpoint**: `GET /api/transactions/user/{userId}`

#### cURL Example
```bash
curl https://api.financial-risk.example.com/api/transactions/user/user_12345 \
  -u username:password
```

---

## Risk Assessment

### Get User Risk Profile

Retrieves comprehensive risk assessment for a user.

**Endpoint**: `GET /api/users/{userId}/risk-profile`

#### cURL Example
```bash
curl https://api.financial-risk.example.com/api/users/user_12345/risk-profile \
  -u username:password
```

#### Response Example
```json
{
  "userId": "user_12345",
  "overallRiskScore": 0.62,
  "behavioralRiskScore": 0.58,
  "transactionRiskScore": 0.67,
  "riskLevel": "MEDIUM",
  "totalTransactions": 128,
  "highRiskTransactions": 3,
  "internationalTransactions": 12,
  "averageTransactionAmount": 356.25,
  "totalTransactionValue": 45600.00,
  "firstTransactionDate": "2025-01-15T10:00:00Z",
  "lastTransactionDate": "2025-11-22T14:30:00Z",
  "accountAgeDays": 312,
  "userType": "REGULAR",
  "profileLastUpdated": "2025-11-22T14:35:00Z"
}
```

### Get Transaction Statistics

Retrieves detailed transaction statistics for a user.

**Endpoint**: `GET /api/users/{userId}/transaction-statistics`

#### Java Example
```java
Mono<TransactionStatisticsDTO> stats = webClient.get()
    .uri("/api/users/{userId}/transaction-statistics", "user_12345")
    .retrieve()
    .bodyToMono(TransactionStatisticsDTO.class);

stats.subscribe(statistics -> {
    System.out.println("Total transactions: " + statistics.getTotalTransactions());
    System.out.println("Average amount: " + statistics.getAverageAmount());
    System.out.println("High risk %: " + statistics.getHighRiskPercentage());

    // Transaction breakdown by merchant category
    statistics.getTransactionsByMerchantCategory().forEach((category, count) -> {
        System.out.println(category + ": " + count + " transactions");
    });
});
```

---

## Fraud Detection

### Get Fraud History for User

Retrieves paginated fraud event history for a user.

**Endpoint**: `GET /api/users/{userId}/fraud-history`

#### cURL Example with Pagination
```bash
curl "https://api.financial-risk.example.com/api/users/user_12345/fraud-history?page=0&size=20&eventType=FRAUD_DETECTED" \
  -u username:password
```

#### Python Example
```python
url = "https://api.financial-risk.example.com/api/users/user_12345/fraud-history"
params = {
    "page": 0,
    "size": 20,
    "eventType": "FRAUD_DETECTED"
}

response = requests.get(url, params=params, auth=auth)
history = response.json()

print(f"Total fraud events: {history['totalFraudEvents']}")
print(f"Fraud detected: {history['fraudDetectedCount']}")
print(f"Transactions blocked: {history['transactionsBlockedCount']}")

for event in history['events']:
    print(f"  - {event['eventType']} at {event['createdAt']}")
    print(f"    Fraud probability: {event['fraudProbability']}")
```

### Get Fraud Events for Transaction

Retrieves all fraud-related events for a specific transaction.

**Endpoint**: `GET /api/transactions/{transactionId}/fraud-events`

---

## Administrative Operations

### Get Flagged Transactions

Retrieves transactions flagged for fraud review.

**Endpoint**: `GET /api/admin/flagged-transactions`

#### cURL Example
```bash
curl "https://api.financial-risk.example.com/api/admin/flagged-transactions?limit=50" \
  -u admin:password
```

#### Response Example
```json
[
  {
    "transactionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "userId": "user_12345",
    "amount": 15000.00,
    "currency": "USD",
    "merchantCategory": "GAMBLING",
    "merchantName": "OnlineCasino.com",
    "flaggedAt": "2025-11-22T14:30:05Z",
    "fraudProbability": 0.87,
    "riskLevel": "HIGH",
    "fraudReasons": [
      "HIGH_AMOUNT_THRESHOLD",
      "HIGH_RISK_MERCHANT",
      "GEOGRAPHIC_ANOMALY"
    ],
    "reviewStatus": "PENDING",
    "daysSinceFlagged": 0
  }
]
```

### Review Flagged Transaction

Submits a review decision for a flagged transaction.

**Endpoint**: `PUT /api/admin/transactions/{transactionId}/review`

#### cURL Example
```bash
curl -X PUT https://api.financial-risk.example.com/api/admin/transactions/f47ac10b-58cc-4372-a567-0e02b2c3d479/review \
  -u admin:password \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVE",
    "reviewerId": "admin_jane_doe",
    "notes": "Customer verified via phone. Legitimate business expense."
  }'
```

#### Java Example
```java
TransactionReviewRequestDTO review = TransactionReviewRequestDTO.builder()
    .decision(ReviewDecision.APPROVE)
    .reviewerId("admin_jane_doe")
    .notes("Customer verified via phone. Legitimate business expense.")
    .build();

webClient.put()
    .uri("/api/admin/transactions/{id}/review", transactionId)
    .bodyValue(review)
    .retrieve()
    .bodyToMono(Map.class)
    .subscribe(response -> {
        System.out.println("Review status: " + response.get("status"));
        System.out.println("Message: " + response.get("message"));
    });
```

### Get Fraud Detection Rules

Retrieves all active fraud detection rules.

**Endpoint**: `GET /api/admin/fraud-rules`

#### Response Example
```json
[
  {
    "ruleId": "RULE_HIGH_AMOUNT",
    "ruleName": "High Amount Transaction",
    "description": "Flags transactions above $10,000",
    "riskWeight": 0.7,
    "isActive": true,
    "category": "AMOUNT",
    "triggerCount": 0
  },
  {
    "ruleId": "RULE_HIGH_RISK_MERCHANT",
    "ruleName": "High Risk Merchant Category",
    "description": "Flags gambling, adult content, and cryptocurrency merchants",
    "riskWeight": 0.8,
    "isActive": true,
    "category": "BEHAVIOR",
    "triggerCount": 0
  }
]
```

---

## Kafka Event Integration

### Publishing Events (Spring Kafka)

```java
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransactionCreated(Transactions transaction) {
        TransactionCreatedEvent event = TransactionCreatedEvent.fromTransaction(transaction);

        kafkaTemplate.send("transaction-events",
            transaction.getId().toString(),
            event);

        log.info("Published TransactionCreatedEvent: {}", event.getEventId());
    }

    public void publishFraudDetected(FraudDetectedEvent event) {
        kafkaTemplate.send("fraud-events",
            event.getTransactionId().toString(),
            event);

        log.info("Published FraudDetectedEvent: probability={}",
            event.getFraudProbability());
    }
}
```

### Consuming Events (Spring Kafka)

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FraudEventConsumer {

    @KafkaListener(topics = "fraud-events", groupId = "fraud-alert-service")
    public void handleFraudDetected(FraudDetectedEvent event) {
        log.info("Processing FraudDetectedEvent: transactionId={}, probability={}",
            event.getTransactionId(), event.getFraudProbability());

        // Send alert to fraud investigation team
        if (event.getFraudProbability() > 0.8) {
            sendHighPriorityAlert(event);
        }

        // Update user risk score
        updateUserRiskProfile(event.getUserId(), event);

        // Send user notification for suspicious activity
        if ("REVIEW".equals(event.getAction())) {
            notifyUser(event.getUserId(), event.getTransactionId());
        }
    }

    @KafkaListener(topics = "transaction-events", groupId = "analytics-service")
    public void processTransactionAnalytics(TransactionCreatedEvent event) {
        log.info("Processing transaction for analytics: {}", event.getTransactionId());

        // Process transaction patterns for ML model training
        collectTrainingData(event);

        // Update real-time analytics dashboard
        updateDashboard(event);
    }
}
```

### Reactive Kafka Consumer

```java
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

@Service
@RequiredArgsConstructor
public class ReactiveEventConsumer {

    private final KafkaReceiver<String, FraudDetectedEvent> receiver;

    @PostConstruct
    public void startConsuming() {
        receiver.receive()
            .doOnNext(record -> {
                FraudDetectedEvent event = record.value();
                processFraudEvent(event)
                    .doOnSuccess(result -> record.receiverOffset().acknowledge())
                    .doOnError(error -> log.error("Failed to process event", error))
                    .subscribe();
            })
            .subscribe();
    }

    private Mono<Void> processFraudEvent(FraudDetectedEvent event) {
        return Mono.fromRunnable(() -> {
            log.info("Processing fraud event: {}", event.getEventId());
            // Business logic here
        });
    }
}
```

---

## Complete Workflow Example

Here's a complete example showing the full lifecycle of a transaction:

```java
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TransactionWorkflowExample {

    private final WebClient webClient;

    public Mono<Void> completeTransactionWorkflow() {
        // Step 1: Create transaction
        return createTransaction()
            // Step 2: Check transaction status
            .flatMap(transaction -> getTransactionStatus(transaction.getId()))
            // Step 3: Handle fraud detection
            .flatMap(status -> {
                if ("FLAGGED".equals(status.getFraudStatus())) {
                    log.warn("Transaction flagged for fraud: {}", status.getTransactionId());
                    return handleFlaggedTransaction(status);
                }
                return Mono.just(status);
            })
            // Step 4: Get updated user risk profile
            .flatMap(status -> getUserRiskProfile(status.getUserId()))
            .doOnSuccess(profile -> {
                log.info("User risk level: {}", profile.getRiskLevel());
                log.info("Overall risk score: {}", profile.getOverallRiskScore());
            })
            .then();
    }

    private Mono<Transactions> createTransaction() {
        TransactionDTO request = TransactionDTO.builder()
            .userId("user_12345")
            .amount(new BigDecimal("5000.00"))
            .currency("USD")
            .transactionType(TransactionType.PURCHASE)
            .merchantCategory("ELECTRONICS")
            .merchantName("BestBuy.com")
            .isInternational(false)
            .build();

        return webClient.post()
            .uri("/api/transactions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Transactions.class);
    }

    private Mono<TransactionStatusDTO> getTransactionStatus(UUID transactionId) {
        return webClient.get()
            .uri("/api/transactions/{id}", transactionId)
            .retrieve()
            .bodyToMono(TransactionStatusDTO.class);
    }

    private Mono<TransactionStatusDTO> handleFlaggedTransaction(TransactionStatusDTO status) {
        log.info("Handling flagged transaction: {}", status.getTransactionId());
        // Additional fraud investigation logic
        return Mono.just(status);
    }

    private Mono<UserRiskProfileDTO> getUserRiskProfile(String userId) {
        return webClient.get()
            .uri("/api/users/{userId}/risk-profile", userId)
            .retrieve()
            .bodyToMono(UserRiskProfileDTO.class);
    }
}
```

---

## Error Handling

### HTTP Error Codes

| Code | Description | Example Response |
|------|-------------|------------------|
| 400 | Bad Request | Invalid transaction data |
| 401 | Unauthorized | Authentication required |
| 403 | Forbidden | Admin access required |
| 404 | Not Found | Resource not found |
| 500 | Internal Server Error | Unexpected error |
| 503 | Service Unavailable | Database connection failed |

### Error Response Format

```json
{
  "timestamp": "2025-11-22T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid transaction amount: must be positive",
  "path": "/api/transactions"
}
```

### Handling Errors in Java

```java
webClient.post()
    .uri("/api/transactions")
    .bodyValue(request)
    .retrieve()
    .onStatus(HttpStatus::is4xxClientError, response ->
        response.bodyToMono(String.class)
            .flatMap(body -> Mono.error(new BadRequestException(body)))
    )
    .onStatus(HttpStatus::is5xxServerError, response ->
        Mono.error(new ServiceUnavailableException("Service temporarily unavailable"))
    )
    .bodyToMono(Transactions.class);
```

---

## API Documentation

### Access Interactive Documentation

Once the application is running, access the Swagger UI at:
- **Development**: http://localhost:8080/swagger-ui.html
- **Production**: https://api.financial-risk.example.com/swagger-ui.html

### OpenAPI Specification

Access the raw OpenAPI specification (JSON) at:
- http://localhost:8080/v3/api-docs

---

## Rate Limiting

API endpoints are subject to rate limiting:
- **Standard users**: 100 requests per minute
- **Admin users**: 500 requests per minute

Rate limit headers are included in responses:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1700668800
```

---

## Support

For additional help:
- **Documentation**: See [KAFKA_EVENTS.md](./KAFKA_EVENTS.md) for event schemas
- **Issues**: Report issues at https://github.com/nickfallico/financial-risk-management/issues
- **Email**: support@financial-risk.example.com
