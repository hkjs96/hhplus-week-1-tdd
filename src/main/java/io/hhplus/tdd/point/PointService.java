package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    private final UserPointTable userPointRepository;
    private final PointHistoryTable pointHistoryRepository;

    public PointService(UserPointTable userPointRepository, PointHistoryTable pointHistoryRepository) {
        this.userPointRepository = userPointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    UserPoint charge(long id,  long amount) {
        UserPoint userPoint = userPointRepository.selectById(id);

        userPoint = userPointRepository.insertOrUpdate(id, userPoint.point() + amount);

        return userPoint;
    }

    UserPoint point(long id) {
        return userPointRepository.selectById(id);
    }

    UserPoint use(long id, long amount) {
        UserPoint userPoint = userPointRepository.selectById(id);

        if (userPoint.point() < amount) {
            throw new InsufficientPointException(userPoint.point(), amount);
        }

        UserPoint updatedUserPoint = userPointRepository.insertOrUpdate(id, userPoint.point() - amount);

        return updatedUserPoint;
    }

    List<PointHistory> history(long id) {
        return pointHistoryRepository.selectAllByUserId(id);
    }
}
