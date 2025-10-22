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
    void 특정사용자_포인트_사용() {
        pointController.use(사용자_ID, 사용할_포인트);

        assertEquals(기대값_ID, 사용자_ID);
        assertEquals(기대값_남은_포인트, 남은_포인트);
    }
}
