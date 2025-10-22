package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class PointServiceTest {

    private UserPointTable userPointTable;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointService = new PointService(userPointTable);
    }

    @Test
    void 특정사용자에게_특정포인트를_충전() {
        UserPoint userPoint = pointService.charge(1L, 1000L);

        assertEquals(1L, userPoint.id());
        assertEquals(1000L, userPoint.point());
    }

    @Test
    void 이미_존재하는_특정_사용자에게_포인트를_충전() {
        // 2 번 유저는 500포인트를 가지고 있어서 1000 포인트 충전시 1500 포인트
        pointService.charge(2L, 500L);
        UserPoint userPoint = pointService.charge(2L, 1000L);

        assertEquals(2L, userPoint.id());
        assertEquals(1500L, userPoint.point());
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

    @Test
    void 특정사용자의_포인트사용() {
        // Given - 사용자에게 포인트 충전
        pointService.charge(1L, 1000L);

        // When - 포인트 사용
        UserPoint userPoint = pointService.use(1L, 300L);

        // Then - 검증
        assertEquals(1L, userPoint.id());
        assertEquals(700L, userPoint.point()); // 1000 - 300 = 700

        // Given - 사용자에게 포인트 충전
        pointService.charge(2L, 2000L);

        // When - 포인트 사용
        UserPoint userPoint2 = pointService.use(2L, 500L);

        // Then - 검증
        assertEquals(2L, userPoint2.id());
        assertEquals(1_500L, userPoint2.point()); // 2000 - 500 = 1500
    }

    @Test
    void 포인트가_부족한_경우_예외_발생() {
        // Given - 사용자는 포인트가 없음 (0 포인트)

        // When & Then - 포인트 사용 시 예외 발생
        InsufficientPointException exception = assertThrows(
            InsufficientPointException.class,
            () -> pointService.use(3L, 500L)
        );

        assertTrue(exception.getMessage().contains("포인트가 부족합니다"));

        // Given - 사용자에게 100 포인트 충전
        pointService.charge(4L, 100L);

        // When & Then - 500 포인트 사용 시도 시 예외 발생
        InsufficientPointException exception2 = assertThrows(
            InsufficientPointException.class,
            () -> pointService.use(4L, 500L)
        );

        assertTrue(exception2.getMessage().contains("포인트가 부족합니다"));
        assertTrue(exception2.getMessage().contains("100"));
        assertTrue(exception2.getMessage().contains("500"));

        // Given - 사용자에게 1000 포인트 충전
        pointService.charge(5L, 1000L);

        // When & Then - 2000 포인트 사용 시도 시 예외 발생
        InsufficientPointException exception3 = assertThrows(
            InsufficientPointException.class,
            () -> pointService.use(5L, 2000L)
        );

        assertTrue(exception3.getMessage().contains("포인트가 부족합니다"));
        assertTrue(exception3.getMessage().contains("1000"));
        assertTrue(exception3.getMessage().contains("2000"));
    }

}