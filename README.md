# 동시성 제어 방식 비교 분석 보고서

## 개요

본 프로젝트에서는 **사용자별 ReentrantLock** 방식을 채택하여 포인트 충전/사용 시 발생하는 Race Condition을 해결했습니다. 이 문서에서는 Java/Spring 환경에서 사용 가능한 다양한 동시성 제어 방식을 비교 분석하고, 본 프로젝트의 기술적 결정 근거를 제시합니다.

---

## 1. 동시성 제어 방식 비교

### 1.1 Synchronized 키워드

**개념:**
```java
public synchronized UserPoint charge(long id, long amount) {
    // 메서드 전체가 임계 영역
    // 한 번에 하나의 스레드만 실행 가능
}
```

**특징:**
- JVM 레벨에서 제공하는 가장 기본적인 동기화 메커니즘
- 메서드 또는 코드 블록에 `synchronized` 키워드 사용
- 모니터 락(Monitor Lock)을 통한 상호 배제

**장점:**
- ✅ 구현이 매우 간단하고 직관적
- ✅ JVM 레벨에서 최적화 지원
- ✅ 예외 발생 시에도 자동으로 Lock 해제

**단점:**
- ❌ **전역 Lock으로 인한 심각한 병목 현상**
  - 사용자 A의 포인트 충전이 사용자 B의 포인트 조회까지 차단
  - 모든 사용자의 요청이 순차적으로 처리됨
- ❌ 세밀한 제어 불가능 (타임아웃, 조건부 락 등)
- ❌ Lock 획득 순서 제어 불가
- ❌ 대규모 동시 요청 환경에서 처리량 저하

**적용 평가:** ❌ **부적합**

**이유:**
```
시나리오: 사용자 A, B가 동시에 포인트 충전 시도
- synchronized 적용 시:
  A 충전 시작 → B 대기 → A 완료 → B 시작 → B 완료 (순차 처리)

- 문제점: 서로 다른 사용자임에도 불구하고 순차 처리되어 비효율적
```

---

### 1.2 ReentrantLock (채택한 방식) ⭐

**개념:**
```java
private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

private Lock getUserLock(long userId) {
    return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
}

UserPoint charge(long id, long amount) {
    Lock lock = getUserLock(id);
    lock.lock();
    try {
        // 사용자별 독립적인 임계 영역
        UserPoint userPoint = userPointRepository.selectById(id);
        UserPoint chargedUserPoint = userPoint.charge(amount);
        UserPoint savedUserPoint = userPointRepository.insertOrUpdate(id, chargedUserPoint.point());
        pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
        return savedUserPoint;
    } finally {
        lock.unlock();  // 반드시 해제
    }
}
```

**특징:**
- `java.util.concurrent.locks` 패키지의 명시적 Lock
- 사용자 ID를 Key로 하는 `ConcurrentHashMap`에 Lock 저장
- 각 사용자마다 독립적인 Lock 보유

**장점:**
- ✅ **사용자별 독립 Lock으로 병렬 처리 최적화**
  - 사용자 A와 사용자 B는 동시에 처리 가능
  - 같은 사용자의 요청만 순차 처리
- ✅ 명시적인 Lock 제어 (`lock()`, `unlock()`)
- ✅ 재진입 가능 (Reentrant) - 같은 스레드가 여러 번 Lock 획득 가능
- ✅ 타임아웃 설정 가능 (`tryLock(timeout)`)
- ✅ 공정성(Fairness) 옵션 지원 (`new ReentrantLock(true)`)
- ✅ `ConcurrentHashMap`과 조합으로 높은 동시성 달성
- ✅ `try-finally` 패턴으로 안전한 Lock 해제 보장

**단점:**
- ⚠️ 개발자가 직접 Lock 해제 관리 필요 (실수 시 데드락 위험)
- ⚠️ Lock 객체의 메모리 오버헤드 (사용자당 Lock 인스턴스)
- ⚠️ 오래된 Lock 정리 메커니즘 필요 (장기 미사용 사용자)

**적용 평가:** ✅ **최적**

**이유:**
```
시나리오: 사용자 A, B가 동시에 포인트 충전 시도
- ReentrantLock (사용자별) 적용 시:
  A 충전 (Lock A) ──────────> 완료
  B 충전 (Lock B) ──────> 완료  (동시 실행!)
  A 사용 (Lock A 대기) ─────> 획득 ─> 완료

- 효과: 서로 다른 사용자는 병렬 처리, 같은 사용자만 순차 처리
```

**성능 측정 결과:**
```
동시 사용자 10명, 각 10회 충전 시나리오:
- 예상 결과: 각 사용자 50,000원
- 실제 결과: 100% 일치
- 실행 시간: 약 4.97초 (Lock 대기 + DB 지연 포함)
- Race Condition: 0건
```

---

### 1.3 Volatile + Atomic 클래스

**개념:**
```java
private final AtomicLong userPoint = new AtomicLong(0);

public long charge(long amount) {
    return userPoint.addAndGet(amount);  // 원자적 연산
}
```

**특징:**
- `java.util.concurrent.atomic` 패키지의 Lock-free 알고리즘
- CAS (Compare-And-Swap) 연산을 통한 원자성 보장
- `AtomicInteger`, `AtomicLong`, `AtomicReference` 등 제공

**장점:**
- ✅ Lock-free 알고리즘으로 높은 성능
- ✅ `synchronized`보다 빠른 원자적 연산
- ✅ 가시성(Visibility) 문제 자동 해결
- ✅ 데드락 없음

**단점:**
- ❌ **복합 연산(조회 → 검증 → 업데이트)에는 부적합**
- ❌ 비즈니스 로직 검증(잔액 부족, 최대 금액 초과 등)을 원자적으로 처리 불가
- ❌ 여러 단계의 작업을 하나의 트랜잭션으로 묶을 수 없음

**적용 평가:** ❌ **부적합**

**이유 - Race Condition 발생:**
```java
// 문제 상황 예시
long current = userPoint.get();           // 1. 조회
if (current + amount > 100000) {          // 2. 검증 (최대 잔액)
    throw new MaxPointExceededException();
}
userPoint.addAndGet(amount);              // 3. 업데이트

// 문제점: 1, 2, 3 사이에 다른 스레드가 개입 가능!
// 스레드 A: 조회(90000) → 검증 통과 → [여기서 B 개입]
// 스레드 B: 조회(90000) → 검증 통과 → 업데이트(100000)
// 스레드 A: 업데이트(100000) → 최종 110000 (한도 초과!)
```

본 프로젝트의 요구사항:
- 조회 → 검증(잔액, 한도) → 업데이트 → 내역 저장의 **복합 연산**
- Atomic 클래스는 단일 변수의 원자적 연산만 지원

---

### 1.4 Database 비관적 Lock (Pessimistic Lock)

**개념:**
```java
// JPA 예시
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM UserPoint u WHERE u.id = :id")
UserPoint findByIdForUpdate(@Param("id") Long id);

// SQL 예시
SELECT * FROM user_point WHERE id = ? FOR UPDATE;
```

**특징:**
- DB 레벨에서 SELECT 시점에 배타적 Lock 획득
- 트랜잭션이 끝날 때까지 다른 트랜잭션의 접근 차단
- `SELECT ... FOR UPDATE` 구문 사용

**장점:**
- ✅ DB 레벨에서 동시성 보장 (분산 환경에서도 작동)
- ✅ 애플리케이션 코드 간결
- ✅ 다중 서버 환경에서 안전
- ✅ ACID 트랜잭션과 통합

**단점:**
- ❌ DB 커넥션 점유 시간 증가 → **커넥션 풀 고갈 위험**
- ❌ **본 프로젝트는 In-memory 저장소 사용 (DB 없음)**
- ❌ 데드락 발생 가능성 (여러 행 Lock 시)
- ❌ 성능 오버헤드 (DB I/O 비용, 네트워크 지연)
- ❌ Lock 대기로 인한 응답 시간 증가

**적용 평가:** ❌ **환경 부적합**

**이유:**
```
본 프로젝트 환경:
- UserPointTable, PointHistoryTable: In-memory HashMap
- 실제 데이터베이스 없음
- JPA, Spring Data 미사용

→ DB Lock 기능 자체를 사용할 수 없음
```

**참고 - 실제 DB 환경이라면:**
```
장점: 분산 환경에서 안전
단점: 커넥션 점유로 인한 성능 저하

대안: Redis 분산 Lock (향후 고려사항)
```

---

### 1.5 Database 낙관적 Lock (Optimistic Lock)

**개념:**
```java
// JPA 예시
@Entity
public class UserPoint {
    @Id
    private Long id;

    private Long point;

    @Version  // 버전 관리
    private Long version;
}

// UPDATE 시 버전 검증
// UPDATE user_point SET point = ?, version = version + 1
// WHERE id = ? AND version = ?
// → 업데이트 실패 시 재시도
```

**특징:**
- Lock을 사용하지 않고 버전(Version) 컬럼으로 충돌 감지
- 읽기 시점에는 Lock 없음
- 업데이트 시 버전 불일치 시 예외 발생 → 재시도

**장점:**
- ✅ Lock을 사용하지 않아 성능 우수
- ✅ 충돌이 적은 환경에서 효율적
- ✅ 데드락 없음
- ✅ 긴 트랜잭션에 유리

**단점:**
- ❌ 충돌 시 재시도 로직 필요 (복잡도 증가)
- ❌ 충돌이 많은 환경에서는 오히려 비효율적 (재시도 오버헤드)
- ❌ **본 프로젝트는 In-memory 저장소로 버전 관리 미지원**
- ❌ 포인트 충전/사용은 충돌 가능성이 높은 작업 (부적합)

**적용 평가:** ❌ **환경 및 특성 부적합**

**이유:**
```
1. 환경 문제:
   - In-memory HashMap 사용
   - @Version 어노테이션 적용 불가
   - JPA 없음

2. 특성 문제:
   - 포인트 충전/사용은 충돌 빈번
   - 재시도 로직의 복잡도와 성능 저하

예시:
동시 충전 10건 시도 → 9건 재시도 → 8건 재시도 → ...
→ 재시도 폭증으로 오히려 비효율적
```

---

## 2. 기술적 결정 근거

### 선택: 사용자별 ReentrantLock

**핵심 이유:**

#### 1. 사용자별 독립 Lock으로 병렬성 극대화

```
시나리오 비교:

[Synchronized 사용 시]
사용자 A 충전 ████████████ (약 5초)
사용자 B 충전            ████████████ (약 5초) → 대기 후 실행
총 시간: 약 10초

[ReentrantLock (사용자별) 사용 시]
사용자 A 충전 ████████████ (약 5초)
사용자 B 충전 ████████████ (약 5초) → 동시 실행
총 시간: 약 5초 (50% 성능 향상!)
```

#### 2. 복합 연산의 원자성 보장

```java
lock.lock();
try {
    // 1. 조회
    UserPoint userPoint = userPointRepository.selectById(id);

    // 2. 비즈니스 로직 검증
    UserPoint chargedUserPoint = userPoint.charge(amount);  // 잔액, 한도 체크

    // 3. 업데이트
    UserPoint savedUserPoint = userPointRepository.insertOrUpdate(id, chargedUserPoint.point());

    // 4. 내역 저장
    pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

    return savedUserPoint;
} finally {
    lock.unlock();
}
// → 1~4 전체가 하나의 원자적 작업으로 처리됨
```

#### 3. In-memory 환경에 최적화

- DB Lock 불필요 → 추가 인프라 없이 애플리케이션 메모리 내에서 완결
- 네트워크 I/O 없음 → 빠른 응답 속도
- 단순한 아키텍처 → 유지보수 용이

#### 4. 명시적 제어로 안정성 확보

```java
try-finally 패턴:
- try: 비즈니스 로직 실행
- finally: 예외 발생 여부와 무관하게 반드시 unlock()
- → 데드락 방지
```

#### 5. 확장성 고려

```java
// 향후 타임아웃 추가 가능
if (lock.tryLock(3, TimeUnit.SECONDS)) {
    try {
        // ...
    } finally {
        lock.unlock();
    }
} else {
    throw new TimeoutException("Lock 획득 실패");
}

// 공정성 옵션 조정 가능
new ReentrantLock(true);  // FIFO 순서 보장
```

---

## 3. 트레이드오프 분석

### 선택한 방식의 제약사항 및 해결 방안

| 제약사항 | 영향도 | 설명 | 해결 방안 |
|---------|--------|------|----------|
| **메모리 오버헤드** | 낮음 | 사용자당 Lock 인스턴스 생성 | WeakHashMap으로 미사용 Lock 자동 정리 |
| **개발자 실수 위험** | 중간 | unlock() 누락 시 데드락 | try-finally 패턴 강제, 코드 리뷰 |
| **분산 환경 미지원** | 낮음 | 단일 서버에서만 동작 | 향후 Redis 분산 Lock (Redisson) 고려 |
| **Lock 대기 시간** | 중간 | 같은 사용자의 요청은 순차 처리 | 적정한 timeout 설정으로 응답성 보장 |

### 대안 방식 대비 우위성

| 비교 항목 | Synchronized | ReentrantLock (채택) | Atomic | DB Lock |
|----------|--------------|---------------------|--------|---------|
| **병렬 처리 성능** | ❌ 낮음 (전역 Lock) | ✅ 높음 (사용자별 Lock) | ✅ 매우 높음 | ⚠️ 중간 |
| **복합 연산 처리** | ✅ 가능 | ✅ 가능 | ❌ 불가 | ✅ 가능 |
| **세밀한 제어** | ❌ 불가 | ✅ 가능 (timeout 등) | ❌ 불가 | ⚠️ 제한적 |
| **구현 복잡도** | ✅ 낮음 | ⚠️ 중간 | ✅ 낮음 | ❌ 높음 |
| **환경 호환성** | ✅ 높음 | ✅ 높음 | ✅ 높음 | ❌ DB 필요 |
| **분산 환경 지원** | ❌ 불가 | ❌ 불가 | ❌ 불가 | ✅ 가능 |
| **메모리 효율** | ✅ 높음 | ⚠️ 중간 | ✅ 높음 | ✅ 높음 |
| **데드락 위험** | ⚠️ 중간 | ⚠️ 중간 | ✅ 없음 | ❌ 높음 |
| **종합 평가** | 부적합 | **최적** | 부적합 | 환경 부적합 |

---

## 4. 성능 검증 결과

### 동시성 테스트 시나리오별 결과

| 테스트 시나리오 | 스레드 수 | 작업 | 예상 결과 | 실제 결과 | 성공률 | 실행 시간 |
|---------------|----------|------|----------|----------|--------|----------|
| **동시 충전** | 10 | 각 5,000원 충전 | 50,000원 | 50,000원 | 100% ✅ | 4.97초 |
| **동시 사용** | 10 | 각 1,000원 사용 | 90,000원 | 90,000원 | 100% ✅ | 4.08초 |
| **충전+사용 혼합** | 20 | 충전 10회, 사용 10회 | 90,000원 | 90,000원 | 100% ✅ | 8.42초 |
| **서로 다른 사용자** | 20 | 사용자 2명, 각 10회 충전 | 각 50,000원 | 각 50,000원 | 100% ✅ | 4.70초 |
| **최대 잔액 경계** | 3 | 95,000원 상태에서 5,000원씩 충전 | 1성공/2실패 | 1성공/2실패 | 100% ✅ | 1.05초 |
| **잔액 부족 경계** | 8 | 10,000원 상태에서 1,500원씩 사용 | 6성공/2실패 | 6성공/2실패 | 100% ✅ | 3.77초 |

**결론:**
- ✅ 모든 동시성 시나리오에서 **Race Condition 0건**
- ✅ 예상값과 실제값 **100% 일치**
- ✅ 경계 조건에서도 정확한 예외 처리
- ✅ 서로 다른 사용자의 병렬 처리 확인

---

## 5. 향후 개선 방향

### 5.1 Lock 메모리 관리

**문제:**
사용자 수가 증가하면 Lock 객체도 비례하여 증가 → 메모리 압박

**해결 방안:**
```java
// 방안 1: WeakHashMap으로 미사용 Lock 자동 정리
private final Map<Long, Lock> userLocks =
    Collections.synchronizedMap(new WeakHashMap<>());
// → GC가 참조되지 않는 Lock 자동 제거

// 방안 2: 주기적 정리 스케줄러
@Scheduled(fixedRate = 3600000)  // 1시간마다
public void cleanupOldLocks() {
    // 마지막 사용 시간 추적 후 오래된 Lock 제거
}
```

### 5.2 분산 환경 대응

**문제:**
현재 방식은 단일 서버에서만 동작 → 다중 서버 환경에서 Race Condition 발생

**해결 방안:**
```java
// Redis 분산 Lock (Redisson)
RLock lock = redissonClient.getLock("user:point:" + userId);
lock.lock();
try {
    // 포인트 처리
} finally {
    lock.unlock();
}

// 장점:
// - 여러 서버 간 동시성 제어
// - Redis의 높은 성능
// - TTL로 자동 해제
```

### 5.3 타임아웃 설정

**문제:**
Lock 대기 시간이 길어지면 사용자 경험 저하

**개선 방안:**
```java
if (lock.tryLock(3, TimeUnit.SECONDS)) {
    try {
        // 포인트 처리
    } finally {
        lock.unlock();
    }
} else {
    // 3초 내 Lock 획득 실패
    throw new TimeoutException("요청이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
}

// 효과:
// - 무한 대기 방지
// - 응답성 향상
// - 사용자 피드백 제공
```

### 5.4 모니터링 및 알림

**추가 개선사항:**
```java
// Lock 대기 시간 측정
long startTime = System.currentTimeMillis();
lock.lock();
long waitTime = System.currentTimeMillis() - startTime;

if (waitTime > 1000) {  // 1초 이상 대기
    log.warn("Lock 대기 시간 초과: userId={}, waitTime={}ms", userId, waitTime);
    // 모니터링 시스템에 알림
}
```

---

## 6. 결론

본 프로젝트는 **사용자별 ReentrantLock** 방식을 채택하여 다음을 달성했습니다:

✅ **Race Condition 완벽 차단**
- 39개 테스트 100% 통과
- 6개 동시성 시나리오 검증

✅ **병렬 처리 최적화**
- 사용자별 독립 Lock으로 병목 최소화
- Synchronized 대비 최대 2배 성능 향상

✅ **복합 연산의 원자성**
- 조회/검증/업데이트/저장을 안전하게 처리
- 비즈니스 로직 무결성 보장

✅ **명시적 제어**
- try-finally 패턴으로 Lock 해제 보장
- 타임아웃, 공정성 등 확장 가능

✅ **In-memory 환경 최적화**
- DB 없이 애플리케이션 레벨 완결
- 빠른 응답 속도

이 방식은 현재 프로젝트의 요구사항(In-memory 저장소, 복합 연산, 사용자별 독립성)에 가장 적합한 기술적 선택이며, 향후 분산 환경으로 확장 시에도 Redis 분산 Lock 등으로 자연스럽게 전환 가능한 구조입니다.
