package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

@Service
public class PointService {

    private final UserPointTable userPointRepository;

    public PointService(UserPointTable userPointRepository) {
        this.userPointRepository = userPointRepository;
    }

    UserPoint charge(long id,  long amount) {
        UserPoint userPoint = userPointRepository.selectById(id);

        userPoint = userPointRepository.insertOrUpdate(id, userPoint.point() + amount);

        return userPoint;
    }
}
