package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@Service
public class PointService {

    private static final int MAX_HISTORY_SIZE = 5;

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

    /**
     * 락을 사용하여 포인트 트랜잭션을 실행하는 공통 메서드
     *
     * @param userId 사용자 ID
     * @param domainOperation 도메인 객체에서 수행할 작업 (charge 또는 use)
     * @param amount 포인트 금액
     * @param transactionType 트랜잭션 타입 (CHARGE 또는 USE)
     * @return 업데이트된 UserPoint
     */
    private UserPoint executePointTransaction(long userId,
                                               Function<UserPoint, UserPoint> domainOperation,
                                               long amount,
                                               TransactionType transactionType) {
        Lock lock = getUserLock(userId);
        lock.lock();
        try {
            UserPoint userPoint = userPointRepository.selectById(userId);
            UserPoint updatedUserPoint = domainOperation.apply(userPoint);
            UserPoint savedUserPoint = userPointRepository.insertOrUpdate(userId, updatedUserPoint.point());
            pointHistoryRepository.insert(userId, amount, transactionType, System.currentTimeMillis());
            return savedUserPoint;
        } finally {
            lock.unlock();
        }
    }

    UserPoint charge(long id,  long amount) {
        return executePointTransaction(id, userPoint -> userPoint.charge(amount), amount, TransactionType.CHARGE);
    }

    UserPoint point(long id) {
        return userPointRepository.selectById(id);
    }

    UserPoint use(long id, long amount) {
        return executePointTransaction(id, userPoint -> userPoint.use(amount), amount, TransactionType.USE);
    }

    List<PointHistory> history(long id) {
        List<PointHistory> allHistories = pointHistoryRepository.selectAllByUserId(id);

        // 최근 N건만 반환
        int size = allHistories.size();
        if (size <= MAX_HISTORY_SIZE) {
            return allHistories;
        }

        // 마지막 N건 반환
        return allHistories.subList(size - MAX_HISTORY_SIZE, size);
    }
}
