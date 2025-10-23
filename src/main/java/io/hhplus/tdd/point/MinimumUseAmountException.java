package io.hhplus.tdd.point;

public class MinimumUseAmountException extends RuntimeException {

    public MinimumUseAmountException(String message) {
        super(message);
    }

    public MinimumUseAmountException(long amount, long minimumAmount) {
        super(String.format("포인트는 최소 %d부터 사용 가능합니다. 입력된 금액: %d", minimumAmount, amount));
    }
}
