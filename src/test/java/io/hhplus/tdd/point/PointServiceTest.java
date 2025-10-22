package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource({
            "1, 2000",
            "2, 500",
            "3, 3000"
    })
    void 특정사용자_포인트_정보_조회(long userId, long chargeAmount) {
        // Given - 테스트 데이터 준비
        pointService.charge(userId, chargeAmount);

        // When - 포인트 조회
        UserPoint userPoint = pointService.point(userId);

        // Then - 검증
        assertEquals(userId, userPoint.id());
        assertEquals(chargeAmount, userPoint.point());
    }

    @Test
    @Disabled
    void 특정사용자의_포인트사용() {
//        UserPoint userPoint = pointService.use();

//        assertEquals();
    }

}