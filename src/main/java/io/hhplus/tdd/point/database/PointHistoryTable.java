package io.hhplus.tdd.point.database;


import org.springframework.stereotype.Component;

import io.hhplus.tdd.point.model.PointHistory;
import io.hhplus.tdd.point.model.TransactionType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 해당 Table 클래스는 변경하지 않고 공개된 API 만을 사용해 데이터를 제어합니다.
 */
@Component
public class PointHistoryTable {
    private final List<PointHistory> table = new ArrayList<>();
    private long cursor = 1;

    public PointHistory insert(long userId, long amount, TransactionType type, long updateMillis) {
        throttle(300L);
        PointHistory pointHistory = new PointHistory(cursor++, userId, amount, type, updateMillis);
        table.add(pointHistory);
        return pointHistory;
    }

    public List<PointHistory> selectAllByUserId(long userId) {
        return table.stream().filter(pointHistory -> pointHistory.userId() == userId).toList();
    }

    // 인메모리 형태라 테스트가 비현실적일 수 있기 때문에 랜덤한 딜레이 처리
    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}
