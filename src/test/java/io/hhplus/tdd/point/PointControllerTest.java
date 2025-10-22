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

    @Test
    void 특정사용자_포인트_사용_케이스1() {
        // Given
        when(pointService.use(1L, 300L))
                .thenReturn(new UserPoint(1L, 700L, System.currentTimeMillis()));

        // When
        UserPoint result = pointController.use(1L, 300L);

        // Then
        assertEquals(1L, result.id());
        assertEquals(700L, result.point());
        verify(pointService).use(1L, 300L);
    }

    @Test
    void 특정사용자_포인트_사용_케이스2() {
        // Given
        when(pointService.use(2L, 500L))
                .thenReturn(new UserPoint(2L, 1500L, System.currentTimeMillis()));

        // When
        UserPoint result = pointController.use(2L, 500L);

        // Then
        assertEquals(2L, result.id());
        assertEquals(1500L, result.point());
        verify(pointService).use(2L, 500L);
    }

    @Test
    void 특정사용자_포인트_사용_케이스3() {
        // Given
        when(pointService.use(3L, 1000L))
                .thenReturn(new UserPoint(3L, 4000L, System.currentTimeMillis()));

        // When
        UserPoint result = pointController.use(3L, 1000L);

        // Then
        assertEquals(3L, result.id());
        assertEquals(4000L, result.point());
        verify(pointService).use(3L, 1000L);
    }
}
