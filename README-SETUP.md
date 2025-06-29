# Cohort Management Service - Setup Guide

This guide provides instructions on how to set up and run the Cohort Management Service locally using LocalStack.

## Prerequisites

- Java 11 or higher
- Maven
- Docker and Docker Compose

## Setup

### 1. Clone the Repository

```bash
git clone https://github.com/iit2010130/CohortManagementService
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

### 1. Create DynamoDB Tables

First, verify that the tables have been created properly:

```bash
# List all DynamoDB tables
aws dynamodb list-tables --endpoint-url http://localhost:4566

# Describe the Customers table
aws dynamodb describe-table --table-name Customers --endpoint-url http://localhost:4566

# Describe the Cohorts table
aws dynamodb describe-table --table-name Cohorts --endpoint-url http://localhost:4566
```

### 2. Add Test Data

You can add test data directly to DynamoDB:

```bash
# Create a test customer
aws dynamodb put-item \
  --table-name Customers \
  --item '{
    "customerId": {"S": "customer123"},
    "dailySpend": {"N": "6000.0"},
    "userType": {"S": "PAID"}
  }' \
  --endpoint-url http://localhost:4566
```

### 3. Send a Message to SQS

You can use the AWS CLI to send a message to the SQS queue:

```bash
# Get the queue URL
QUEUE_URL=$(aws sqs get-queue-url --queue-name customer-data-queue --endpoint-url http://localhost:4566 --query 'QueueUrl' --output text)

# Send a message to the queue
aws sqs send-message \
  --queue-url $QUEUE_URL \
  --message-body '{"customerId":"customer123","dailySpend":6000.0,"userType":"PAID"}' \
  --endpoint-url http://localhost:4566

# Wait for processing
sleep 5
```

### 4. Test the Three Main Queries

#### Query 1: Determine if a Customer is in a Cohort

```bash
# Using the REST API
curl -X GET "http://localhost:8080/api/cohorts/check?customerId=customer123&cohortType=PREMIUM"

# Expected response: true (since dailySpend > 5000)
# Note: You might see a '%' at the end of the output - this is just your terminal prompt
# If you want to avoid this, use: curl -X GET "http://localhost:8080/api/cohorts/check?customerId=customer123&cohortType=PREMIUM" -w '\n'
```

#### Query 2: List All Cohorts for a Customer

```bash
# Using the REST API
curl -X GET "http://localhost:8080/api/cohorts/customer/customer123"

# Expected response: A list containing the PREMIUM cohort
```

#### Query 3: Get All Customers in a Cohort Type

```bash
# Using the REST API
curl -X GET "http://localhost:8080/api/cohorts/type/PREMIUM/customers"

# Expected response: A set containing "customer123"
```

### 5. Verify Data in DynamoDB

```bash
# Check the Cohorts table
aws dynamodb scan --table-name Cohorts --endpoint-url http://localhost:4566

# Check the Customers table
aws dynamodb scan --table-name Customers --endpoint-url http://localhost:4566
```

### 6. Restart Testing (Optional)

If you want to delete the tables and restart the test:

```bash
# Delete the tables
aws dynamodb delete-table --table-name Customers --endpoint-url http://localhost:4566
aws dynamodb delete-table --table-name Cohorts --endpoint-url http://localhost:4566

# Delete the queue (optional)
QUEUE_URL=$(aws sqs get-queue-url --queue-name customer-data-queue --endpoint-url http://localhost:4566 --query 'QueueUrl' --output text)
aws sqs delete-queue --queue-url $QUEUE_URL --endpoint-url http://localhost:4566

# Restart the application to recreate the tables and queue
# First stop the current application (Ctrl+C in the terminal where it's running)
# Then restart it:
mvn spring-boot:run
```

After restarting the application, you can repeat steps 1-5 to test again with fresh tables.

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
