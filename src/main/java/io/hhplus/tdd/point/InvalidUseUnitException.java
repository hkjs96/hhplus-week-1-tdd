package io.hhplus.tdd.point;

public class InvalidUseUnitException extends RuntimeException {

    public InvalidUseUnitException(String message) {
        super(message);
    }

    public InvalidUseUnitException(long amount, long unit) {
        super(String.format("포인트는 %d 단위로만 사용 가능합니다. 입력된 금액: %d", unit, amount));
    }
}
