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

## Development Notes

### Current State

The controller endpoints are scaffolded with TODO comments but return empty/placeholder data. Implementation is needed for:
1. Retrieving user point balances
2. Retrieving point transaction history
3. Charging user points
4. Using/deducting user points

### TDD Approach

This project emphasizes Test-Driven Development. When implementing features:
1. Write tests first before implementation
2. Use the database table APIs (`UserPointTable`, `PointHistoryTable`) without modifying them
3. Tests should account for the simulated latency in database operations
4. The test task is configured with `ignoreFailures = true` in build.gradle.kts

### Dependencies

Key dependencies:
- Spring Boot Starter Web
- Lombok (for reducing boilerplate)
- Spring Boot Configuration Processor
- Spring Cloud (via dependency management)
- JaCoCo (code coverage)
