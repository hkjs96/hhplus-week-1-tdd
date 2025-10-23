# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a TDD (Test-Driven Development) practice project for implementing a user point management system. It's a Spring Boot application built with Gradle that manages user points including charging, using, and tracking point transactions.

**Project Name:** hhplus-tdd-jvm
**Group:** io.hhplus.tdd
**Java Version:** 17
**Build Tool:** Gradle with Kotlin DSL

## Build and Test Commands

### Building the project
```bash
./gradlew build
```

### Running tests
```bash
./gradlew test
```

### Running the application
```bash
./gradlew bootRun
```

### Running a single test class
```bash
./gradlew test --tests "FullyQualifiedClassName"
```

### Running a specific test method
```bash
./gradlew test --tests "FullyQualifiedClassName.methodName"
```

### Clean build
```bash
./gradlew clean build
```

### Code coverage (JaCoCo)
```bash
./gradlew test jacocoTestReport
```

## Architecture

This is a Spring Boot REST API application with a layered architecture focused on TDD practices.

### Package Structure

- **`io.hhplus.tdd.point`** - Point domain logic (controllers, models, business logic)
- **`io.hhplus.tdd.database`** - In-memory database tables with simulated latency
- **`io.hhplus.tdd`** - Application entry point and global exception handling

### Key Components

**Controllers:**
- `PointController` - REST API endpoints for point operations at `/point`

**Services:**
- `PointService` - Business logic layer for point operations (currently empty, needs implementation)

**Domain Models (Java Records):**
- `UserPoint` - User point balance and update timestamp
- `PointHistory` - Point transaction history record
- `TransactionType` - Enum for CHARGE/USE operations

**Database Tables (In-Memory with Simulated Latency):**
- `UserPointTable` - Stores user point balances (200-300ms random latency)
- `PointHistoryTable` - Stores point transaction history (300ms random latency)

**IMPORTANT:** The database table classes (`UserPointTable`, `PointHistoryTable`) must NOT be modified. Only use their public APIs to interact with data. These tables simulate database latency with random delays.

### API Endpoints

All endpoints are under `/point`:

- `GET /point/{id}` - Get user point balance
- `GET /point/{id}/histories` - Get user point transaction history
- `PATCH /point/{id}/charge` - Charge points (request body: amount as long)
- `PATCH /point/{id}/use` - Use points (request body: amount as long)

### Exception Handling

Global exception handling is configured in `ApiControllerAdvice` which catches all exceptions and returns a 500 error with a generic Korean error message.

## Business Rules

### Point Charging Rules (UserPoint.charge)
- **Unit Constraint**: Must be in 5,000 won increments
- **Positive Amount**: Amount must be greater than 0
- **Maximum Balance**: Total balance cannot exceed 100,000 won
- **Validation Exceptions**:
  - `InvalidPointAmountException`: When amount ≤ 0
  - `InvalidChargeUnitException`: When amount is not a multiple of 5,000
  - `MaxPointExceededException`: When balance would exceed 100,000

### Point Usage Rules (UserPoint.use)
- **Unit Constraint**: Must be in 100 won increments
- **Minimum Amount**: Must be at least 500 won
- **Positive Amount**: Amount must be greater than 0
- **Sufficient Balance**: User must have enough points
- **Validation Exceptions**:
  - `InvalidPointAmountException`: When amount ≤ 0
  - `InvalidUseUnitException`: When amount is not a multiple of 100
  - `MinimumUseAmountException`: When amount < 500
  - `InsufficientPointException`: When balance < amount

### Point History Rules
- Only the **most recent 5 transactions** are returned when querying history
- Both CHARGE and USE transactions are recorded with timestamps

## Development Notes

### Current Implementation Status

✅ **Completed (Step 1)**:
- UserPoint domain model with business rules
- PointService delegating to domain methods
- PointController REST endpoints
- Unit tests for all components
- Point history with 5-record limit

🔄 **In Progress (Step 2)**:
- Integration tests
- Concurrency control
- Test coverage analysis

### TDD Workflow

When implementing new features, follow this workflow:

1. **Write Test First**
   ```bash
   # Create test in src/test/java/io/hhplus/tdd/point/
   # Run: ./gradlew test --tests "YourTestClass"
   ```

2. **Implement Minimum Code**
   - Make the test pass with simplest implementation
   - Keep business logic in domain models (UserPoint)
   - Service layer orchestrates and delegates to domain

3. **Refactor**
   - Use custom command: `/refactor-check`
   - Ensure tests still pass after refactoring

4. **Verify Integration**
   - Use custom command: `/api-test`
   - Test actual HTTP endpoints

### Using Custom Commands

This project includes helpful slash commands in `.claude/commands/`:

- `/test-and-verify` - Run all tests and analyze results
- `/refactor-check` - Verify refactoring didn't break anything
- `/api-test` - Test all API endpoints from point-api.http
- `/coverage` - Generate and analyze code coverage report

### Important Constraints

- **DO NOT modify** `UserPointTable` or `PointHistoryTable` classes
- **Use database table APIs** as-is (they simulate real database latency)
- **Keep business logic** in domain models (UserPoint), not in Service layer
- **Service layer** should orchestrate and delegate, not implement business rules

### Dependencies

Key dependencies:
- Spring Boot Starter Web
- Lombok (for reducing boilerplate)
- Spring Boot Configuration Processor
- Spring Cloud (via dependency management)
- JaCoCo (code coverage)
