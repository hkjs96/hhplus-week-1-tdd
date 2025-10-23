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

        // 포인트 충전 내역 저장
        pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

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

        // 포인트 사용 내역 저장
        pointHistoryRepository.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

        return updatedUserPoint;
    }

    List<PointHistory> history(long id) {
        List<PointHistory> allHistories = pointHistoryRepository.selectAllByUserId(id);

        // 최근 5건만 반환
        int size = allHistories.size();
        if (size <= 5) {
            return allHistories;
        }

        // 마지막 5건 반환
        return allHistories.subList(size - 5, size);
    }
}
