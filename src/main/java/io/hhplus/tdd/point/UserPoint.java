package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    // 상수 선언
    private static final long CHARGE_UNIT = 5_000L;
    private static final long USE_UNIT = 100L;
    private static final long MAX_POINT = 100_000L;
    private static final long MIN_USE_AMOUNT = 500L;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint charge(long amount) {
        validatePositiveAmount(amount);
        validateChargeUnit(amount);
        validateMaxBalance(amount);

        return new UserPoint(this.id, this.point + amount, System.currentTimeMillis());
    }

    public UserPoint use(long amount) {
        validatePositiveAmount(amount);
        validateUseUnit(amount);
        validateMinimumUseAmount(amount);
        validateSufficientBalance(amount);

        return new UserPoint(this.id, this.point - amount, System.currentTimeMillis());
    }

    // 검증 메서드들
    private void validatePositiveAmount(long amount) {
        if (amount <= 0) {
            throw new InvalidPointAmountException(amount);
        }
    }

    private void validateChargeUnit(long amount) {
        if (amount % CHARGE_UNIT != 0) {
            throw new InvalidChargeUnitException(amount, CHARGE_UNIT);
        }
    }

    private void validateMaxBalance(long amount) {
        if (this.point + amount > MAX_POINT) {
            throw new MaxPointExceededException(this.point, amount, MAX_POINT);
        }
    }

    private void validateUseUnit(long amount) {
        if (amount % USE_UNIT != 0) {
            throw new InvalidUseUnitException(amount, USE_UNIT);
        }
    }

    private void validateMinimumUseAmount(long amount) {
        if (amount < MIN_USE_AMOUNT) {
            throw new MinimumUseAmountException(amount, MIN_USE_AMOUNT);
        }
    }

    private void validateSufficientBalance(long amount) {
        if (this.point < amount) {
            throw new InsufficientPointException(this.point, amount);
        }
    }
}
