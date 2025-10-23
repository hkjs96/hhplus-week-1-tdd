package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Mock/Stub을 활용한 PointService 단위 테스트
 * Repository 의존성을 Mock으로 대체하여 Service 로직만 독립적으로 검증
 */
@ExtendWith(MockitoExtension.class)
class PointServiceMockTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    private PointService pointService;

    @BeforeEach
    void setUp() {
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Test
    void 포인트_충전_시_Repository_호출_검증() {
        // Given
        long userId = 1L;
        long amount = 5000L;
        UserPoint existingPoint = new UserPoint(userId, 0L, System.currentTimeMillis());
        UserPoint expectedPoint = new UserPoint(userId, 5000L, System.currentTimeMillis());

        // Mock 설정: selectById 호출 시 existingPoint 반환
        when(userPointTable.selectById(userId)).thenReturn(existingPoint);
        // Mock 설정: insertOrUpdate 호출 시 expectedPoint 반환
        when(userPointTable.insertOrUpdate(eq(userId), eq(5000L))).thenReturn(expectedPoint);

        // When
        UserPoint result = pointService.charge(userId, amount);

        // Then
        assertEquals(5000L, result.point());

        // Repository 메서드 호출 검증
        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, times(1)).insertOrUpdate(userId, 5000L);
        verify(pointHistoryTable, times(1)).insert(
                eq(userId),
                eq(amount),
                eq(TransactionType.CHARGE),
                anyLong()
        );
    }

    @Test
    void 포인트_사용_시_Repository_호출_검증() {
        // Given
        long userId = 1L;
        long amount = 1000L;
        UserPoint existingPoint = new UserPoint(userId, 5000L, System.currentTimeMillis());
        UserPoint expectedPoint = new UserPoint(userId, 4000L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(existingPoint);
        when(userPointTable.insertOrUpdate(eq(userId), eq(4000L))).thenReturn(expectedPoint);

        // When
        UserPoint result = pointService.use(userId, amount);

        // Then
        assertEquals(4000L, result.point());

        // Repository 메서드 호출 검증
        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, times(1)).insertOrUpdate(userId, 4000L);
        verify(pointHistoryTable, times(1)).insert(
                eq(userId),
                eq(amount),
                eq(TransactionType.USE),
                anyLong()
        );
    }

    @Test
    void 포인트_조회_시_Repository_호출_검증() {
        // Given
        long userId = 1L;
        UserPoint expectedPoint = new UserPoint(userId, 10000L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(expectedPoint);

        // When
        UserPoint result = pointService.point(userId);

        // Then
        assertEquals(10000L, result.point());
        verify(userPointTable, times(1)).selectById(userId);
        verifyNoInteractions(pointHistoryTable); // 내역 테이블은 호출되지 않아야 함
    }

    @Test
    void 포인트_내역_조회_시_Repository_호출_검증() {
        // Given
        long userId = 1L;
        List<PointHistory> mockHistories = List.of(
                new PointHistory(1L, userId, 5000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 1000L, TransactionType.USE, System.currentTimeMillis())
        );

        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(mockHistories);

        // When
        List<PointHistory> result = pointService.history(userId);

        // Then
        assertEquals(2, result.size());
        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
        verifyNoInteractions(userPointTable); // 포인트 테이블은 호출되지 않아야 함
    }

    @Test
    void 잔액_부족_시_Repository_업데이트_호출되지_않음() {
        // Given
        long userId = 1L;
        long amount = 10000L;
        UserPoint existingPoint = new UserPoint(userId, 5000L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(existingPoint);

        // When & Then
        assertThrows(InsufficientPointException.class, () -> {
            pointService.use(userId, amount);
        });

        // Repository 업데이트는 호출되지 않아야 함
        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    void 잘못된_충전_단위_시_Repository_업데이트_호출되지_않음() {
        // Given
        long userId = 1L;
        long invalidAmount = 3000L; // 5000원 단위 아님
        UserPoint existingPoint = new UserPoint(userId, 0L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(existingPoint);

        // When & Then
        assertThrows(InvalidChargeUnitException.class, () -> {
            pointService.charge(userId, invalidAmount);
        });

        // Repository 업데이트는 호출되지 않아야 함
        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    void 포인트_내역_5건_제한_로직_검증() {
        // Given - 7건의 내역이 있는 상황
        long userId = 1L;
        List<PointHistory> mockHistories = List.of(
                new PointHistory(1L, userId, 5000L, TransactionType.CHARGE, 1000L),
                new PointHistory(2L, userId, 1000L, TransactionType.USE, 2000L),
                new PointHistory(3L, userId, 5000L, TransactionType.CHARGE, 3000L),
                new PointHistory(4L, userId, 2000L, TransactionType.USE, 4000L),
                new PointHistory(5L, userId, 5000L, TransactionType.CHARGE, 5000L),
                new PointHistory(6L, userId, 3000L, TransactionType.USE, 6000L),
                new PointHistory(7L, userId, 5000L, TransactionType.CHARGE, 7000L)
        );

        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(mockHistories);

        // When
        List<PointHistory> result = pointService.history(userId);

        // Then - 최근 5건만 반환되어야 함
        assertEquals(5, result.size());
        assertEquals(3L, result.get(0).id()); // 3번째 내역부터
        assertEquals(7L, result.get(4).id()); // 7번째 내역까지

        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
    }

    @Test
    void Repository가_null을_반환하면_NPE_발생() {
        // Given
        long userId = 999L;
        when(userPointTable.selectById(userId)).thenReturn(null);

        // When & Then - NullPointerException 발생
        assertThrows(NullPointerException.class, () -> {
            pointService.charge(userId, 5000L);
        });

        verify(userPointTable, times(1)).selectById(userId);
    }
}
