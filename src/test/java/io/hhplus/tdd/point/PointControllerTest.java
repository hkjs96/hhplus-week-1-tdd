package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        when(pointService.charge(1L, 5_000L))
                .thenReturn(new UserPoint(1L, 5_000L, System.currentTimeMillis()));

        UserPoint result = pointController.charge(1L, 5_000L);

        assertEquals(1L, result.id());
        assertEquals(5_000L, result.point());
        verify(pointService).charge(1L, 5_000L);
    }

    @Test
    void 특정사용자의_포인트_조회() {
        when(pointService.point(1L))
                .thenReturn(new UserPoint(1L, 10_000L, System.currentTimeMillis()));

        UserPoint userPoint = pointController.point(1L);

        assertEquals(1L, userPoint.id());
        assertEquals(10_000L, userPoint.point());
        verify(pointService).point(1L);
    }

    @Test
    void 특정사용자_포인트_사용() {
        // Given
        when(pointService.use(1L, 1_300L))
                .thenReturn(new UserPoint(1L, 8_700L, System.currentTimeMillis()));

        // When
        UserPoint result = pointController.use(1L, 1_300L);

        // Then
        assertEquals(1L, result.id());
        assertEquals(8_700L, result.point());
        verify(pointService).use(1L, 1_300L);
    }

    @Test
    void 포인트가_부족한_경우_예외_발생() {
        // Given - 사용자는 3000 포인트를 가지고 있음
        when(pointService.use(1L, 5_000L))
                .thenThrow(new InsufficientPointException(3_000L, 5_000L));

        // When & Then - 5000 포인트를 사용하려고 시도하면 InsufficientPointException 예외가 발생해야 함
        InsufficientPointException exception = assertThrows(
                InsufficientPointException.class,
                () -> pointController.use(1L, 5_000L)
        );

        // Then - 예외 메시지에 포인트 값이 포함되어 있는지 검증
        assertTrue(exception.getMessage().contains("3000"));
        assertTrue(exception.getMessage().contains("5000"));
        verify(pointService).use(1L, 5_000L);
    }

    @Test
    void 사용자포인트내역조회() {
        // Given - 사용자 1번의 포인트 충전/사용 내역 데이터를 준비함
        List<PointHistory> mockHistories = List.of(
                new PointHistory(1L, 1L, 5_000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, 1L, 1_200L, TransactionType.USE, System.currentTimeMillis()),
                new PointHistory(3L, 1L, 5_000L, TransactionType.CHARGE, System.currentTimeMillis())
        );
        when(pointService.history(1L)).thenReturn(mockHistories);

        // When - 사용자 1번의 포인트 내역을 조회함
        List<PointHistory> histories = pointController.history(1L);

        // Then - 조회된 내역이 존재해야 함 (null이 아님)
        assertNotNull(histories);
        // And - 총 3개의 거래 내역이 존재해야 함
        assertEquals(3, histories.size());
        // And - 첫 번째 내역은 5000 포인트 충전(CHARGE)이어야 함
        assertEquals(5_000L, histories.get(0).amount());
        assertEquals(TransactionType.CHARGE, histories.get(0).type());
        // And - 두 번째 내역은 1200 포인트 사용(USE)이어야 함
        assertEquals(1_200L, histories.get(1).amount());
        assertEquals(TransactionType.USE, histories.get(1).type());
        // And - 세 번째 내역은 5000 포인트 충전(CHARGE)이어야 함
        assertEquals(5_000L, histories.get(2).amount());
        assertEquals(TransactionType.CHARGE, histories.get(2).type());
        // And - pointService.history()가 호출되었는지 검증
        verify(pointService).history(1L);
    }
}
