package io.hhplus.tdd.point;

public class MaxPointExceededException extends RuntimeException {

    public MaxPointExceededException(String message) {
        super(message);
    }

    public MaxPointExceededException(long currentPoint, long chargeAmount, long maxPoint) {
        super(String.format("포인트 최대 보유량을 초과할 수 없습니다. 현재: %d, 충전시도: %d, 최대: %d",
                currentPoint, chargeAmount, maxPoint));
    }
}
