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

        // Then - 예외 메시지 검증
        assertTrue(exception.getMessage().contains("포인트가 부족합니다"));
        assertTrue(exception.getMessage().contains(String.valueOf(currentPoint)));
        assertTrue(exception.getMessage().contains(String.valueOf(useAmount)));
    }

}