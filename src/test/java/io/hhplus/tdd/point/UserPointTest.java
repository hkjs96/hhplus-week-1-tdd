package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserPointTest {

    @Test
    void 포인트_충전() {
        UserPoint userPoint = new UserPoint(1L, 1_000L, System.currentTimeMillis());

        UserPoint updateUserPoint = userPoint.charge(500L);

        assertEquals(1_500L, updateUserPoint.point());
    }
}