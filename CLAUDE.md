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

### Creating HTTP Request Files

This project uses `.http` files (IntelliJ HTTP Client format) for manual API testing. The existing `point-api.http` file demonstrates the format.

**When to create `.http` files:**
- When implementing new REST API endpoints
- For manual testing during development
- To document API usage examples
- To validate request/response formats

**Format:**
```http
### Description of the request
METHOD http://localhost:8080/endpoint
Content-Type: application/json

request-body

### Next request
```

**Example (from point-api.http):**
```http
### 포인트 충전
PATCH http://localhost:8080/point/1/charge
Content-Type: application/json

5000

### 포인트 조회
GET http://localhost:8080/point/1

### 포인트 사용
PATCH http://localhost:8080/point/1/use
Content-Type: application/json

1300
```

**Best Practices:**
- Use `###` comments to describe each request
- Include both success and failure cases
- Test edge cases (잘못된 단위, 잔액 부족, etc.)
- Group related requests together
- Use meaningful userId values for testing

**Testing with `.http` files:**
1. In IntelliJ IDEA, click the green arrow next to each request
2. Or use the custom command: `/api-test` to test all endpoints
3. Ensure the application is running: `./gradlew bootRun`

### Maintaining PR Documentation

This project maintains PR documentation files (e.g., `PR_STEP1.md`, `PR_STEP2.md`) to track development progress and commits.

**When to update PR documentation:**
- After completing significant refactoring work
- After implementing new features or fixing bugs
- After completing TDD cycles (Red-Green-Refactor)
- When adding new commits that contribute to the PR's scope

**What to include in PR documentation updates:**
- **Commit hash and link**: Use the format `[commit-hash](github-url)` for traceability
- **Brief description**: One-line summary of what the commit does
- **Detailed notes** (for major changes):
  - Key changes made (e.g., constants extracted, methods refactored)
  - Improvements achieved (e.g., code reduction, readability enhancement)
  - Patterns applied (e.g., DRY principle, functional programming)

**Structure of PR documentation:**
```markdown
#### Section Name (e.g., 리팩토링 및 테스트 확대)
- Brief commit description: [`hash`](url)
  - Detailed change 1
  - Detailed change 2
  - Impact or improvement
```

**Example:**
```markdown
#### 리팩토링 및 테스트 확대 (REFACTOR)
- PointService 중복 코드 제거 및 매직 넘버 추출: [`bae14db`](https://github.com/.../commit/bae14db)
  - 매직 넘버 5를 MAX_HISTORY_SIZE 상수로 추출
  - 중복된 락 패턴을 executePointTransaction 메서드로 통합
  - charge()와 use() 메서드를 각각 20줄에서 1줄로 간소화
  - Function<UserPoint, UserPoint>를 활용한 함수형 프로그래밍 적용
```

**Best Practices:**
- Update PR documentation immediately after completing related commits
- Group related commits under appropriate section headers
- Use clear, descriptive language that explains the "why" not just the "what"
- Include metrics when applicable (e.g., "87줄 → 82줄", "20줄 → 1줄")
- Align section names with TDD phases (RED, GREEN, REFACTOR) when relevant

**Important:**
- Always update CLAUDE.md when establishing new documentation patterns
- This ensures Claude Code can maintain consistency in future PR documentation updates
- Keep PR documentation in sync with actual commits to maintain project traceability

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
