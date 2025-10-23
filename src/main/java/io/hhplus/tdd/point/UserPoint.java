package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint charge(long amount) {
        final long MAX_POINT = 100_000L;

        if(amount <= 0) {
            throw new InvalidPointAmountException(amount);
        }

        if(amount % 5_000 != 0) {
            throw new InvalidChargeUnitException(amount, 5_000);
        }

        if(this.point + amount > MAX_POINT) {
            throw new MaxPointExceededException(this.point, amount, MAX_POINT);
        }

        return new UserPoint (this.id, this.point + amount, System.currentTimeMillis());
    }

    public UserPoint use(long amount) {
        final long USE_UNIT = 100L;
        final long MIN_USE_AMOUNT = 500L;

        if(amount <= 0) {
            throw new InvalidPointAmountException(amount);
        }

        if(amount % USE_UNIT != 0) {
            throw new InvalidUseUnitException(amount, USE_UNIT);
        }

        if(amount < MIN_USE_AMOUNT) {
            throw new MinimumUseAmountException(amount, MIN_USE_AMOUNT);
        }

        if(this.point < amount) {
            throw new InsufficientPointException(this.point, amount);
        }

        return new UserPoint(this.id, this.point - amount, System.currentTimeMillis());
    }
}
