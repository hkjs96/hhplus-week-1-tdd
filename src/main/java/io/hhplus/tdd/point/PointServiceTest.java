package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PointServiceTest {

    @Test
    void 특정사용자에게_특정포인트를_충전() {
        PointService pointService = new PointService();

        UserPoint userPoint = pointService.charge(1, 1000);

        assertEquals(기대하는_사용자_ID, 실제_사용자_ID);
        assertEquals(기대하는_사용자_포인트, 실제_사용자_포인트);
    }
}