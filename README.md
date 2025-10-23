# 항해플러스 TDD 과제: 포인트 관리 시스템

TDD (Test-Driven Development) 방식으로 구현한 사용자 포인트 충전/사용 시스템입니다.

## 📋 목차

- [프로젝트 개요](#프로젝트-개요)
- [기능 명세](#기능-명세)
- [동시성 제어](#동시성-제어)
- [테스트 전략](#테스트-전략)
- [실행 방법](#실행-방법)

---

## 🎯 프로젝트 개요

### 기술 스택
- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Gradle (Kotlin DSL)
- **Test**: JUnit 5, MockMvc
- **Coverage**: JaCoCo

### 아키텍처
```
Controller (API Layer)
    ↓
Service (Orchestration + Concurrency Control)
    ↓
Domain Model (Business Logic)
    ↓
Repository (Data Access)
```

---

## ✅ 기능 명세

### 1. 포인트 충전 (Charge)

**API**: `PATCH /point/{id}/charge`

**비즈니스 규칙**:
- 5,000원 단위로만 충전 가능
- 양수 금액만 허용
- 최대 보유 가능 포인트: 100,000원

**예외**:
- `InvalidPointAmountException`: 음수 또는 0원
- `InvalidChargeUnitException`: 5,000원 단위가 아님
- `MaxPointExceededException`: 최대 잔액 초과

### 2. 포인트 사용 (Use)

**API**: `PATCH /point/{id}/use`

**비즈니스 규칙**:
- 100원 단위로만 사용 가능
- 최소 사용 금액: 500원
- 양수 금액만 허용
- 잔액 부족 시 사용 불가

**예외**:
- `InvalidPointAmountException`: 음수 또는 0원
- `InvalidUseUnitException`: 100원 단위가 아님
- `MinimumUseAmountException`: 500원 미만
- `InsufficientPointException`: 잔액 부족

### 3. 포인트 조회

**API**: `GET /point/{id}`

### 4. 포인트 내역 조회

**API**: `GET /point/{id}/histories`

**특징**: 최근 5건만 반환

---

## 🔒 동시성 제어

### 문제 상황

여러 스레드가 동시에 같은 사용자의 포인트를 충전하거나 사용할 때 **Race Condition**이 발생할 수 있습니다.

**예시**:
```
초기 잔액: 0원
스레드 A: 5000원 충전 시도
스레드 B: 5000원 충전 시도

Race Condition 발생 시:
- 두 스레드 모두 잔액 0원을 읽음
- 두 스레드 모두 5000원으로 업데이트
- 최종 잔액: 5000원 (예상: 10000원)
```

### 해결 방법: ReentrantLock

**구현 방식**: 사용자 ID별 Lock 관리

```java
@Service
public class PointService {
    private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

    private Lock getUserLock(long userId) {
        return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
    }

    UserPoint charge(long id, long amount) {
        Lock lock = getUserLock(id);
        lock.lock();
        try {
            // 포인트 충전 로직
            // ...
        } finally {
            lock.unlock();
        }
    }
}
```

### 동시성 제어 특징

#### 1. **사용자별 독립적인 Lock**
- `ConcurrentHashMap`을 사용하여 사용자 ID별로 개별 Lock 관리
- 다른 사용자의 포인트 작업은 서로 영향을 주지 않음
- 사용자 A의 Lock이 사용자 B의 작업을 차단하지 않음

#### 2. **ReentrantLock 사용 이유**
- `synchronized`보다 더 명시적이고 유연한 제어 가능
- `try-finally` 블록으로 안전한 Lock 해제 보장
- 동일 스레드에서 재진입 가능 (Reentrant)

#### 3. **임계 영역 (Critical Section)**
```java
lock.lock();  // Lock 획득
try {
    // 1. 포인트 조회
    // 2. 비즈니스 로직 검증
    // 3. 포인트 업데이트
    // 4. 내역 저장
} finally {
    lock.unlock();  // 반드시 Lock 해제
}
```

### 동시성 테스트 결과

**테스트 시나리오**:
1. 10개 스레드가 동시에 5000원씩 충전
2. 10개 스레드가 동시에 1000원씩 사용
3. 충전과 사용이 혼합된 동시 실행

**결과**: 모든 시나리오에서 예상값과 실제값 일치 ✅

```
=== 동시성 테스트 결과 ===
성공 횟수: 10
실패 횟수: 0
예상 포인트: 50000
실제 포인트: 50000
```

### 성능 고려사항

**장점**:
- 사용자별 Lock으로 병목 현상 최소화
- 다른 사용자 작업과 독립적으로 실행
- 명시적인 Lock 관리로 디버깅 용이

**주의사항**:
- Lock을 획득한 스레드는 작업 완료 시까지 다른 스레드 대기
- `finally` 블록에서 반드시 `unlock()` 호출 필요
- 데드락 방지를 위해 Lock 순서 일관성 유지

---

## 🧪 테스트 전략

### 테스트 구조

#### 1. **단위 테스트 (Unit Test)**
- `UserPointTest`: 도메인 모델 비즈니스 규칙 검증 (7개 테스트)
- `PointServiceTest`: 서비스 레이어 로직 검증 (6개 테스트)
- `PointServiceMockTest`: Mock/Stub을 활용한 서비스 격리 테스트 (8개 테스트)
- `PointControllerTest`: API 엔드포인트 검증 (5개 테스트)

#### 2. **통합 테스트 (Integration Test)**
- `PointIntegrationTest`: 전체 레이어 통합 검증 (7개 테스트)
  - 기본 흐름: 충전, 사용, 조회
  - 예외 처리: 잘못된 단위, 잔액 부족
  - 경계 조건: 최대 잔액, 최소 사용 금액, 내역 5건 제한
- Spring Context 로드
- MockMvc를 사용한 API 테스트

#### 3. **동시성 테스트 (Concurrency Test)**
- `PointConcurrencyTest`: Race Condition 검증 (6개 테스트)
  - 동시 충전/사용 기본 시나리오
  - 충전과 사용 혼합 시나리오
  - 서로 다른 사용자 독립적인 Lock 검증
  - 최대 잔액 초과 동시 시도 (부분 성공/실패)
  - 잔액 부족 상황 동시 사용 (부분 성공/실패)
- `ExecutorService`와 `CountDownLatch` 사용
- 여러 스레드 동시 실행 시나리오

### 테스트 통계

**총 테스트**: 39개 (100% 성공)
- 단위 테스트: 26개
- 통합 테스트: 7개
- 동시성 테스트: 6개

**전체 커버리지**: 94%
- **라인 커버리지**: 94%
- **브랜치 커버리지**: 100%
- **io.hhplus.tdd.point 패키지**: 94%

```bash
# 커버리지 리포트 생성
./gradlew test jacocoTestReport

# 리포트 확인
open build/reports/jacoco/test/html/index.html
```

---

## 🚀 실행 방법

### 프로젝트 빌드
```bash
./gradlew build
```

### 애플리케이션 실행
```bash
./gradlew bootRun
```

### 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests "io.hhplus.tdd.point.PointIntegrationTest"

# 동시성 테스트
./gradlew test --tests "io.hhplus.tdd.point.PointConcurrencyTest"
```

### API 테스트

#### 포인트 충전
```bash
curl -X PATCH http://localhost:8080/point/1/charge \
  -H "Content-Type: application/json" \
  -d "5000"
```

#### 포인트 사용
```bash
curl -X PATCH http://localhost:8080/point/1/use \
  -H "Content-Type: application/json" \
  -d "1300"
```

#### 포인트 조회
```bash
curl http://localhost:8080/point/1
```

#### 포인트 내역 조회
```bash
curl http://localhost:8080/point/1/histories
```

---

## 📊 TDD 프로세스

### Red-Green-Refactor 사이클

1. **RED**: 실패하는 테스트 작성
2. **GREEN**: 테스트를 통과하는 최소한의 코드 작성
3. **REFACTOR**: 코드 개선 및 중복 제거

### 커밋 히스토리

- `RED: 통합 테스트 작성` - 전체 흐름 검증 테스트
- `RED: 동시성 문제 재현 테스트 작성` - Race Condition 재현
- `GREEN: 동시성 제어 구현` - ReentrantLock 적용

---

## 🤖 AI 협업

이 프로젝트는 **Claude Code**를 활용하여 개발되었습니다.

### Custom Commands
- `/test-and-verify` - 테스트 실행 및 분석
- `/refactor-check` - 리팩토링 검증
- `/api-test` - API 통합 테스트
- `/coverage` - 커버리지 분석

### CLAUDE.md
프로젝트 컨텍스트와 가이드라인을 `CLAUDE.md`에 문서화하여 일관된 개발 프로세스를 유지했습니다.

---

## 📝 라이센스

이 프로젝트는 항해플러스 백엔드 코스의 TDD 실습 과제입니다.
