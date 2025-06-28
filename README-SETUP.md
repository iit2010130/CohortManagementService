# Cohort Management Service - Setup Guide

This guide provides instructions on how to set up and run the Cohort Management Service locally using LocalStack.

## Prerequisites

- Java 11 or higher
- Maven
- Docker and Docker Compose

## Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd CohortManagementService
```

### 2. Start LocalStack

LocalStack is used to emulate AWS services locally for development and testing.

```bash
cd localstack
docker-compose up -d
```

This will start LocalStack with DynamoDB and SQS services.

### 3. Build the Service

```bash
mvn clean install
```

### 4. Run the Service

```bash
mvn spring-boot:run
```

The service will start on port 8080 by default.

## Testing the Service

### 1. Send a Message to SQS

You can use the AWS CLI with the `--endpoint-url` parameter to interact with LocalStack:

```bash
aws --endpoint-url=http://localhost:4566 sqs send-message \
  --queue-url http://localhost:4566/000000000000/customer-data-queue \
  --message-body '{"customerId":"123","dailySpend":6000,"userType":"PAID"}'
```

### 2. Check the Cohort Classification

You can use the REST API to check if a customer is in a specific cohort type:

```bash
curl -X GET "http://localhost:8080/api/cohorts/check?customerId=123&cohortType=PREMIUM"
```

Or to get all cohorts for a customer:

```bash
curl -X GET "http://localhost:8080/api/cohorts/customer/123"
```

Or to get all customers in a cohort type:

```bash
curl -X GET "http://localhost:8080/api/cohorts/type/PREMIUM/customers"
```

## Architecture

The Cohort Management Service consists of the following components:

1. **Customer Data Ingestion**: Consumes customer data from SQS and stores it in DynamoDB.
2. **DynamoDB Stream Processing**: Processes events from DynamoDB streams to classify customers into cohorts.
3. **Cohort Classification**: Classifies customers into cohorts based on configurable rules.
4. **REST API**: Provides endpoints for querying cohort information.

## Configuration

The service configuration is in `src/main/resources/application.yml`. You can modify this file to change the service configuration, such as the AWS endpoint URL, region, table names, etc.

## Adding New Rules

To add a new rule for cohort classification:

1. Create a new class that implements the `CohortRule` interface.
2. Add the rule to the `cohortRules` bean in `CohortManagementServiceApplication.java`.

Example:

```java
@Bean
public List<CohortRule> cohortRules() {
    List<CohortRule> rules = new ArrayList<>();
    rules.add(new DailySpendRule()); // Add the DailySpend rule with default threshold (5000)
    rules.add(new YourNewRule()); // Add your new rule
    return rules;
}
```

## Troubleshooting

### LocalStack Issues

If you encounter issues with LocalStack, you can check the logs:

```bash
docker logs localstack
```

### Service Issues

Check the service logs for any errors:

```bash
tail -f logs/cohort-management-service.log
```

## Additional Resources

- [LocalStack Documentation](https://docs.localstack.cloud/)
- [AWS SDK for Java Documentation](https://docs.aws.amazon.com/sdk-for-java/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
