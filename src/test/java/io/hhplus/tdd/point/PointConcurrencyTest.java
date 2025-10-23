package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

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
}
