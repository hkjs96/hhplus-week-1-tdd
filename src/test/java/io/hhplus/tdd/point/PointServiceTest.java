package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PointServiceTest {

    private UserPointTable userPointRepository;
    private PointHistoryTable pointHistoryRepository;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        userPointRepository = new UserPointTable();
        pointHistoryRepository = new PointHistoryTable();
        pointService = new PointService(userPointRepository, pointHistoryRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 0, 1000, 1000",      // userId, 기존포인트, 충전포인트, 예상포인트
            "2, 500, 1000, 1500"
    })
    void 특정사용자에게_특정포인트를_충전(long userId, long initialPoint, long chargeAmount, long expectedPoint) {
        // Given - 기존 포인트가 있는 경우 먼저 충전
        if (initialPoint > 0) {
            pointService.charge(userId, initialPoint);
        }

        // When - 포인트 충전
        UserPoint userPoint = pointService.charge(userId, chargeAmount);

        // Then - 검증
        assertEquals(userId, userPoint.id());
        assertEquals(expectedPoint, userPoint.point());
    }

    @ParameterizedTest
    @CsvSource({
            "1, 2000",
            "2, 500",
            "3, 3000"
    })
    void 특정사용자_포인트_정보_조회(long userId, long chargeAmount) {
        // Given - 테스트 데이터 준비
        pointService.charge(userId, chargeAmount);

        // When - 포인트 조회
        UserPoint userPoint = pointService.point(userId);

        // Then - 검증
        assertEquals(userId, userPoint.id());
        assertEquals(chargeAmount, userPoint.point());
    }

    @ParameterizedTest
    @CsvSource({
            "1, 1000, 300, 700",    // userId, 충전포인트, 사용포인트, 예상잔액
            "2, 2000, 500, 1500"
    })
    void 특정사용자의_포인트사용(long userId, long chargeAmount, long useAmount, long expectedPoint) {
        // Given - 사용자에게 포인트 충전
        pointService.charge(userId, chargeAmount);

        // When - 포인트 사용
        UserPoint userPoint = pointService.use(userId, useAmount);

        // Then - 검증
        assertEquals(userId, userPoint.id());
        assertEquals(expectedPoint, userPoint.point());
    }

    @ParameterizedTest
    @CsvSource({
            "3, 0, 500",      // userId, 현재포인트, 사용시도포인트
            "4, 100, 500",
            "5, 1000, 2000"
    })
    void 포인트가_부족한_경우_예외_발생(long userId, long currentPoint, long useAmount) {
        // Given - 사용자에게 포인트 충전 (0이 아닌 경우)
        if (currentPoint > 0) {
            pointService.charge(userId, currentPoint);
        }

        // When & Then - 포인트 사용 시도 시 예외 발생
        InsufficientPointException exception = assertThrows(
            InsufficientPointException.class,
            () -> pointService.use(userId, useAmount)
        );

        // Then - 예외 메시지에 포인트 값이 포함되어 있는지 검증
        assertTrue(exception.getMessage().contains(String.valueOf(currentPoint)));
        assertTrue(exception.getMessage().contains(String.valueOf(useAmount)));
    }

    @Test
    void 특정사용자_포인트충전_및_사용내역조회() {
        // Given - 사용자 1번이 다음과 같은 순서로 거래를 진행함:
        pointService.charge(1L, 5_000L);  // 1. 5000 포인트를 충전함
        pointService.use(1L, 500L);       // 2. 500 포인트를 사용함
        pointService.charge(1L, 5_000L);  // 3. 5000 포인트를 충전함

        // When - 사용자 1번의 포인트 충전/사용 내역을 조회함
        List<PointHistory> pointHistories = pointService.history(1L);

        // Then - 조회된 내역이 존재해야 함 (null이 아님)
        assertNotNull(pointHistories);
        // And - 총 3개의 거래 내역이 존재해야 함
        assertEquals(3, pointHistories.size());

        // And - 각 내역 검증
        assertHistory(pointHistories.get(0), 1L, 5_000L, TransactionType.CHARGE);
        assertHistory(pointHistories.get(1), 1L, 500L, TransactionType.USE);
        assertHistory(pointHistories.get(2), 1L, 5_000L, TransactionType.CHARGE);
    }

    @Test
    void 포인트_내역_조회는_최근_5건만_반환() {
        // Given - 사용자 1번이 7건의 거래를 진행함
        pointService.charge(1L, 5_000L);   // 1번째 거래
        pointService.use(1L, 500L);        // 2번째 거래
        pointService.charge(1L, 5_000L);   // 3번째 거래
        pointService.use(1L, 1_000L);      // 4번째 거래
        pointService.charge(1L, 5_000L);   // 5번째 거래
        pointService.use(1L, 500L);        // 6번째 거래
        pointService.charge(1L, 5_000L);   // 7번째 거래

        // When - 사용자 1번의 포인트 내역을 조회함
        List<PointHistory> pointHistories = pointService.history(1L);

        // Then - 조회된 내역은 최근 5건만 반환되어야 함
        assertEquals(5, pointHistories.size());

        // And - 최근 5건만 포함되어야 함 (3번째부터 7번째 거래)
        assertHistory(pointHistories.get(0), 1L, 5_000L, TransactionType.CHARGE);  // 3번째
        assertHistory(pointHistories.get(1), 1L, 1_000L, TransactionType.USE);     // 4번째
        assertHistory(pointHistories.get(2), 1L, 5_000L, TransactionType.CHARGE);  // 5번째
        assertHistory(pointHistories.get(3), 1L, 500L, TransactionType.USE);       // 6번째
        assertHistory(pointHistories.get(4), 1L, 5_000L, TransactionType.CHARGE);  // 7번째
    }

    private void assertHistory(PointHistory history, long expectedUserId, long expectedAmount, TransactionType expectedType) {
        assertEquals(expectedUserId, history.userId());
        assertEquals(expectedAmount, history.amount());
        assertEquals(expectedType, history.type());
    }
}