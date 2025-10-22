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
        // When - 500 포인트를 사용하려고 시도
        // Then - InsufficientPointException 예외가 발생해야 함
        // And - 예외 메시지에 "포인트가 부족합니다"가 포함되어야 함
        // And - 예외 메시지에 현재 포인트(100)와 요청 포인트(500)가 포함되어야 함
    }
}
