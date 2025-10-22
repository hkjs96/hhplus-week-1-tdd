package io.hhplus.tdd.point;

public class InsufficientPointException extends RuntimeException {

    public InsufficientPointException(String message) {
        super(message);
    }

    public InsufficientPointException(long currentPoint, long requestedAmount) {
        super(String.format("포인트가 부족합니다. 현재 포인트: %d, 요청 포인트: %d", currentPoint, requestedAmount));
    }
}
