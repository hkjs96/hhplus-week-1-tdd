package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserPointTest {

    @Test
    void 포인트_충전() {
        UserPoint userPoint = new UserPoint(1L, 10_000L, System.currentTimeMillis());

        UserPoint updateUserPoint = userPoint.charge(5_000L);

        assertEquals(15_000L, updateUserPoint.point());
    }

    @Test
    void 포인트는_양수() {
        UserPoint userPoint = new UserPoint(1L, 1_000L, System.currentTimeMillis());

        assertThrows(InvalidPointAmountException.class, () -> userPoint.charge(-500L));
    }

    @Test
    void 포인트는_5000원_단위로_충전() {
        UserPoint userPoint = new UserPoint(1L, 1_000L, System.currentTimeMillis());

        assertThrows(InvalidChargeUnitException.class, () -> userPoint.charge(3_000L));
        assertThrows(InvalidChargeUnitException.class, () -> userPoint.charge(7_500L));
        assertThrows(InvalidChargeUnitException.class, () -> userPoint.charge(1_000L));
    }

    @Test
    void 포인트_보유량은_100000_포인트를_초과할_수_없음() {
        UserPoint userPoint = new UserPoint(1L, 95_000L, System.currentTimeMillis());

        assertThrows(MaxPointExceededException.class, () -> userPoint.charge(10_000L));
    }

    @Test
    void 포인트는_100_단위로_사용_가능() {
        UserPoint userPoint = new UserPoint(1L, 10_000L, System.currentTimeMillis());

        assertThrows(InvalidUseUnitException.class, () -> userPoint.use(150L));
        assertThrows(InvalidUseUnitException.class, () -> userPoint.use(555L));
        assertThrows(InvalidUseUnitException.class, () -> userPoint.use(1_250L));
    }

    @Test
    void 포인트는_최소_500_포인트부터_사용_가능() {
        UserPoint userPoint = new UserPoint(1L, 10_000L, System.currentTimeMillis());

        assertThrows(MinimumUseAmountException.class, () -> userPoint.use(400L));
        assertThrows(MinimumUseAmountException.class, () -> userPoint.use(100L));
    }
}