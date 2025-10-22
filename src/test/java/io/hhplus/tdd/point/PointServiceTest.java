package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @Test
    void 특정사용자_포인트_정보_조회() {
        UserPoint userPoint = pointService.point(1L);

        assertEquals(1L, userPoint.id());
        assertEquals(2_000L, userPoint.point());
    }
}