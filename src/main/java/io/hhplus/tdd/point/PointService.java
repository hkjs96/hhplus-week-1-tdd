package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointService {

    private final UserPointTable userPointRepository;
    private final PointHistoryTable pointHistoryRepository;
    private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

    public PointService(UserPointTable userPointRepository, PointHistoryTable pointHistoryRepository) {
        this.userPointRepository = userPointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    private Lock getUserLock(long userId) {
        return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
    }

    UserPoint charge(long id,  long amount) {
        Lock lock = getUserLock(id);
        lock.lock();
        try {
            UserPoint userPoint = userPointRepository.selectById(id);

            // UserPoint 도메인 객체의 charge 메서드를 사용하여 비즈니스 로직 수행
            UserPoint chargedUserPoint = userPoint.charge(amount);

            // 업데이트된 포인트 저장
            UserPoint savedUserPoint = userPointRepository.insertOrUpdate(id, chargedUserPoint.point());

            // 포인트 충전 내역 저장
            pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return savedUserPoint;
        } finally {
            lock.unlock();
        }
    }

    UserPoint point(long id) {
        return userPointRepository.selectById(id);
    }

    UserPoint use(long id, long amount) {
        Lock lock = getUserLock(id);
        lock.lock();
        try {
            UserPoint userPoint = userPointRepository.selectById(id);

            // UserPoint 도메인 객체의 use 메서드를 사용하여 비즈니스 로직 수행
            UserPoint usedUserPoint = userPoint.use(amount);

            // 업데이트된 포인트 저장
            UserPoint savedUserPoint = userPointRepository.insertOrUpdate(id, usedUserPoint.point());

            // 포인트 사용 내역 저장
            pointHistoryRepository.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

            return savedUserPoint;
        } finally {
            lock.unlock();
        }
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
