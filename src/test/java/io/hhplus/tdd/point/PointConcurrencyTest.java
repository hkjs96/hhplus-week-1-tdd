package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 동시성 테스트: 멀티스레드 환경에서 Race Condition 검증
 *
 * ReentrantLock을 사용한 동시성 제어가 올바르게 동작하는지 확인
 */
@DisplayName("포인트 시스템 동시성 테스트")
class PointConcurrencyTest {

    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Test
    @DisplayName("10개 스레드 동시 충전 - Race Condition 방지 검증")
    void 동시에_여러_스레드가_포인트_충전_시도() throws InterruptedException {
        // Given - 사용자 1번에게 초기 포인트 없음
        long userId = 1L;
        int threadCount = 10;
        long chargeAmount = 5000L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 10개 스레드가 동시에 5000원씩 충전
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then - 최종 포인트는 50000원이어야 함 (5000 * 10)
        UserPoint finalPoint = pointService.point(userId);

        System.out.println("=== 동시성 테스트 결과 ===");
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());
        System.out.println("예상 포인트: " + (chargeAmount * threadCount));
        System.out.println("실제 포인트: " + finalPoint.point());

        // 동시성 제어가 없으면 실패할 것으로 예상
        assertEquals(chargeAmount * threadCount, finalPoint.point(),
                "동시성 제어가 없으면 최종 포인트가 기대값과 다를 수 있습니다.");
    }

    @Test
    @DisplayName("10개 스레드 동시 사용 - Race Condition 방지 검증")
    void 동시에_여러_스레드가_포인트_사용_시도() throws InterruptedException {
        // Given - 사용자 2번에게 100000원 충전
        long userId = 2L;
        pointService.charge(userId, 100_000L);

        int threadCount = 10;
        long useAmount = 1000L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 10개 스레드가 동시에 1000원씩 사용
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.use(userId, useAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then - 최종 포인트는 90000원이어야 함 (100000 - 1000 * 10)
        UserPoint finalPoint = pointService.point(userId);

        System.out.println("=== 동시성 테스트 결과 (사용) ===");
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());
        System.out.println("예상 포인트: " + (100_000L - useAmount * threadCount));
        System.out.println("실제 포인트: " + finalPoint.point());

        assertEquals(100_000L - useAmount * threadCount, finalPoint.point(),
                "동시성 제어가 없으면 최종 포인트가 기대값과 다를 수 있습니다.");
    }

    @Test
    @DisplayName("충전과 사용 혼합 - 복잡한 동시성 시나리오 검증")
    void 동시에_충전과_사용이_섞여서_발생() throws InterruptedException {
        // Given - 사용자 3번에게 50000원 충전
        long userId = 3L;
        pointService.charge(userId, 50_000L);

        int threadCount = 20; // 충전 10번, 사용 10번

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When - 10개 스레드는 5000원 충전, 10개 스레드는 1000원 사용
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    if (index < 10) {
                        // 충전
                        pointService.charge(userId, 5000L);
                    } else {
                        // 사용
                        pointService.use(userId, 1000L);
                    }
                } catch (Exception e) {
                    System.err.println("에러 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then - 최종 포인트는 90000원이어야 함
        // 50000 (초기) + 5000 * 10 (충전) - 1000 * 10 (사용) = 90000
        UserPoint finalPoint = pointService.point(userId);
        long expectedPoint = 50_000L + (5000L * 10) - (1000L * 10);

        System.out.println("=== 동시성 테스트 결과 (충전+사용 혼합) ===");
        System.out.println("예상 포인트: " + expectedPoint);
        System.out.println("실제 포인트: " + finalPoint.point());

        assertEquals(expectedPoint, finalPoint.point(),
                "동시성 제어가 없으면 충전과 사용이 섞일 때 문제가 발생할 수 있습니다.");
    }

    @Test
    @DisplayName("서로 다른 사용자 - 독립적인 Lock 검증")
    void 서로_다른_사용자_동시_작업() throws InterruptedException {
        // Given - 두 명의 사용자
        long userId1 = 10L;
        long userId2 = 20L;
        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount * 2);
        CountDownLatch latch = new CountDownLatch(threadCount * 2);

        // When - 두 사용자가 동시에 각각 5000원씩 5번 충전
        for (int i = 0; i < threadCount; i++) {
            // 사용자 1 충전
            executorService.submit(() -> {
                try {
                    pointService.charge(userId1, 5000L);
                } catch (Exception e) {
                    System.err.println("User1 충전 에러: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            // 사용자 2 충전
            executorService.submit(() -> {
                try {
                    pointService.charge(userId2, 5000L);
                } catch (Exception e) {
                    System.err.println("User2 충전 에러: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then - 각 사용자의 최종 포인트가 독립적으로 계산되어야 함
        UserPoint user1Point = pointService.point(userId1);
        UserPoint user2Point = pointService.point(userId2);

        System.out.println("=== 독립적인 Lock 테스트 결과 ===");
        System.out.println("사용자1 포인트: " + user1Point.point());
        System.out.println("사용자2 포인트: " + user2Point.point());

        assertEquals(50000L, user1Point.point(), "사용자1의 포인트가 올바르지 않습니다.");
        assertEquals(50000L, user2Point.point(), "사용자2의 포인트가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("최대 잔액 초과 동시 시도 - 일부 성공/실패 검증")
    void 최대_잔액_근처에서_동시_충전() throws InterruptedException {
        // Given - 사용자에게 95000원 충전 (최대 100000원까지 가능)
        long userId = 30L;
        pointService.charge(userId, 95_000L);

        int threadCount = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 3개 스레드가 동시에 5000원씩 충전 시도 (총 15000원)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, 5000L);
                    successCount.incrementAndGet();
                } catch (MaxPointExceededException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("예상치 못한 에러: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then - 1번만 성공하고 2번은 실패해야 함 (95000 + 5000 = 100000, 이후는 초과)
        UserPoint finalPoint = pointService.point(userId);

        System.out.println("=== 최대 잔액 동시성 테스트 결과 ===");
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());
        System.out.println("최종 포인트: " + finalPoint.point());

        assertEquals(1, successCount.get(), "정확히 1번만 성공해야 합니다.");
        assertEquals(2, failCount.get(), "나머지 2번은 실패해야 합니다.");
        assertEquals(100_000L, finalPoint.point(), "최종 포인트는 최대 잔액이어야 합니다.");
    }

    @Test
    @DisplayName("잔액 부족 상황 동시 사용 - 일부 성공/실패 검증")
    void 잔액_부족_상황에서_동시_사용() throws InterruptedException {
        // Given - 사용자에게 7000원 충전
        long userId = 40L;
        pointService.charge(userId, 5_000L);
        pointService.charge(userId, 5_000L); // 총 10000원

        int threadCount = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 8개 스레드가 동시에 1500원씩 사용 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.use(userId, 1500L);
                    successCount.incrementAndGet();
                } catch (InsufficientPointException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("예상치 못한 에러: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then - 10000 / 1500 = 6번 성공, 2번 실패
        UserPoint finalPoint = pointService.point(userId);

        System.out.println("=== 잔액 부족 동시성 테스트 결과 ===");
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());
        System.out.println("최종 포인트: " + finalPoint.point());

        assertEquals(6, successCount.get(), "6번 성공해야 합니다.");
        assertEquals(2, failCount.get(), "2번 실패해야 합니다.");
        assertEquals(1000L, finalPoint.point(), "최종 포인트는 1000원이어야 합니다.");
    }
}
