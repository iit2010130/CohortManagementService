# CohortManagementService
This service manages customer data from multiple sources and classifies them into different cohorts such as FRAUD, PREMIUM, and NORMAL based on configurable business rules.

# Prompt-Driven Development Process
We will follow a Prompt-Driven Development (PDD) workflow:

1. Review provided resources and draft detailed User Stories.
2. Author validates and signs off the User Stories.
3. Prepare a formal Business Requirement Document (BRD).
4. Upon BRD approval, implement the described features.
5. Deploy the working implementation to GitHub.

### Resources
Problem Description:  https://drive.google.com/file/d/1fVa8KOmNgRmNM9QTZMWpKpCMAGcsKWCd/view?usp=sharing

### Requirements
1. Classify customers into one or multiple cohorts based on data from multiple input sources.
2.  Support data ingestion from:
     1) AWS SQS (queue-based ingestion)
     2) It should support API call for future extension
3. Ensure that cohort classification logic is extensible. Initially, rules may be hardcoded, but the design must support adding rules via configuration files or APIs.


### Input Data Schema
1. CustomerId -> String -> Unique identifier
2. DailySpend -> Double -> Daily spend amount
3. UserType -> ENUM(PAID/FREE)

The schema should be extensible to include additional fields in future without breaking existing functionality.

### Sample Rule Logic
1. If DailySpend > 5000, classify as PREMIUM. We will name this Rule as DailySpend so that Customer can provide this as input source.

Rule engine should be extensible to add more rules without breaking existing functionality. One customer can belong to multiple cohort based on different rules.

### Supported Queries
1. Determine if a given CustomerId is part of a specific cohort.
2. List all cohorts associated with a given CustomerId.
3. Retrieve all CustomerIds for a specific cohort.


### System Components (Local Setup)
The system will use LocalStack to emulate AWS services locally for development and testing.
1. DynamoDB
2. SQS
3. DynamoDB Streams

Setup guide: https://docs.localstack.cloud/user-guide/aws/dynamodb/ 

### Prompt Template for AI-Driven Development
Each development task will follow this prompt format to assist AI-based code generation:

Prompt Template:

Task: [Description of the required feature]

Input:
- Data structure definitions
- API contract (if applicable)
- Business rules
- Dependencies (LocalStack services, external libraries)

Expected Output:
- Production-ready Java code (using specified frameworks/libraries)
- Unit tests
- API documentation annotations (if REST API)
- DynamoDB table schema (if applicable)
- LocalStack configurations (if applicable)

Constraints:
- Follow clean coding principles
- Ensure modular, extensible, and testable code

## Example Prompt:
Task: Implement a service method to classify a customer as PREMIUM if their DailySpend > 5000.

Input:
- Customer object: { CustomerId, DailySpend, UserType }
- Rule: DailySpend > 5000

Expected Output:
- Java service class with a classifyCustomer() method
- Unit tests for the classification logic

## Next Steps
1. Draft the initial set of User Stories.
2. Review and approve User Stories.
3. Generate implementation prompts based on approved User Stories.


### Suggested Directory Structure
/src/main/java/com/cohortmgmt
    /controller
    /service
    /model
    /repository
    /config
/src/test/java/com/cohortmgmt
    /service
    /controller
/resources
    application.yml
    cohort-rules.json
/localstack
    docker-compose.yml
README.md
