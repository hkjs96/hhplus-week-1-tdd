package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PointControllerTest {

    private PointController pointController;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        pointService = mock(PointService.class);
        pointController = new PointController(pointService);
    }

    @Test
    void 포인트_충전_API_호출() {
        when(pointService.charge(1L, 1000L))
                .thenReturn(new UserPoint(1L, 1000L, System.currentTimeMillis()));

        UserPoint result = pointController.charge(1L, 1000L);

        assertEquals(1L, result.id());
        assertEquals(1000L, result.point());
        verify(pointService).charge(1L, 1000L);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 1000",
            "2, 13333000"
    })
    void 특정사용자의_포인트_조회(long userId, long expectedPoint) {
        when(pointService.point(userId))
                .thenReturn(new UserPoint(userId, expectedPoint, System.currentTimeMillis()));

        UserPoint userPoint = pointController.point(userId);

        assertEquals(userId, userPoint.id());
        assertEquals(expectedPoint, userPoint.point());
        verify(pointService).point(userId);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 300, 700",      // userId, 사용포인트, 남은포인트
            "2, 500, 1500",
            "3, 1000, 4000"
    })
    void 특정사용자_포인트_사용(long userId, long useAmount, long remainingPoint) {
        // Given
        when(pointService.use(userId, useAmount))
                .thenReturn(new UserPoint(userId, remainingPoint, System.currentTimeMillis()));

        // When
        UserPoint result = pointController.use(userId, useAmount);

        // Then
        assertEquals(userId, result.id());
        assertEquals(remainingPoint, result.point());
        verify(pointService).use(userId, useAmount);
    }

    @Test
    void 포인트가_부족한_경우_예외_발생() {
        // Given - 사용자는 100 포인트를 가지고 있음
        when(pointService.use(1L, 500L))
                .thenThrow(new InsufficientPointException(100L, 500L));

        // When & Then - 500 포인트를 사용하려고 시도하면 InsufficientPointException 예외가 발생해야 함
        InsufficientPointException exception = assertThrows(
                InsufficientPointException.class,
                () -> pointController.use(1L, 500L)
        );

        // Then - 예외 메시지에 포인트 값이 포함되어 있는지 검증
        assertTrue(exception.getMessage().contains("100"));
        assertTrue(exception.getMessage().contains("500"));
        verify(pointService).use(1L, 500L);
    }

    @Test
    void 사용자포인트내역조회() {
        // Given - 사용자 1번의 포인트 충전/사용 내역 데이터를 준비함
        // - 1000 포인트 충전 내역
        // - 300 포인트 사용 내역
        // - 500 포인트 충전 내역
        // 총 3개의 거래 내역을 Mock으로 준비

        // When - 사용자 1번의 포인트 내역을 조회함
        // List<PointHistory> histories = pointController.history(사용자_ID);

        // Then - 조회된 내역이 존재해야 함 (null이 아님)
        // And - 총 3개의 거래 내역이 존재해야 함
        // And - 첫 번째 내역은 1000 포인트 충전(CHARGE)이어야 함
        // And - 두 번째 내역은 300 포인트 사용(USE)이어야 함
        // And - 세 번째 내역은 500 포인트 충전(CHARGE)이어야 함
        // And - pointService.history()가 호출되었는지 검증
    }
}
