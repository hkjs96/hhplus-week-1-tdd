package io.hhplus.tdd.point;

public class InvalidPointAmountException extends RuntimeException {

    public InvalidPointAmountException(String message) {
        super(message);
    }

    public InvalidPointAmountException(long amount) {
        super(String.format("포인트 금액은 양수여야 합니다. 입력된 금액: %d", amount));
    }
}
