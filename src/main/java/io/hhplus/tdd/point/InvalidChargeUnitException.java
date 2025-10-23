package io.hhplus.tdd.point;

public class InvalidChargeUnitException extends RuntimeException {

    public InvalidChargeUnitException(String message) {
        super(message);
    }

    public InvalidChargeUnitException(long amount, long unit) {
        super(String.format("포인트는 %d원 단위로만 충전 가능합니다. 입력된 금액: %d", unit, amount));
    }
}
