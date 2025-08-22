package io.hhplus.tdd.point.service;

import org.springframework.stereotype.Service;

import io.hhplus.tdd.point.database.PointHistoryTable;
import io.hhplus.tdd.point.database.UserPointTable;
import io.hhplus.tdd.point.model.TransactionType;
import io.hhplus.tdd.point.model.UserPoint;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable PointHistoryTable;

    private final static int MINIMUM_CHARGE_AMOUNT = 1000;

    @Override
    public UserPoint chargePoint(long userId, long amount) {
        checkValidation(userId, amount);

        // 사용자의 이전 포인트 조회
        // 아이디에 대한 결과값이 없어도 포인트는 0
        long prePoint = getPrePoint(userId);

        // 포인트 저장 : 충전 시 이전 포인트 + 충전 포인트
        UserPoint result = userPointTable.insertOrUpdate(userId, prePoint + amount);
        // 포인트 히스토리 저장 : 히스토리는 현재 충전 포인트만
        PointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return result;
    }

    @Override
    public UserPoint usePoint(long userId, long amount) {
        checkValidation(userId, amount);

        long prePoint = getPrePoint(userId);

        if (prePoint < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        // 포인트 사용 : 이전 포인트 - 사용 포인트
        UserPoint result = userPointTable.insertOrUpdate(userId, prePoint - amount);
        // 포인트 히스토리 : 사용 포인트
        PointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

        return result;
    }

    @Override
    public UserPoint selectPointById(long userId) {
        checkValidation_userId(userId);

        return userPointTable.selectById(userId);
    }

    // upset 시 유효성 체크
    private void checkValidation(long userId, long amount) {
        if (userId < 1) {
            throw new IllegalArgumentException("잘못된 사용자 아이디 입니다.");
        }
        if (amount < 0 || amount < MINIMUM_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("충전/사용 금액은 " + MINIMUM_CHARGE_AMOUNT + "원부터 가능합니다.");
        }
    }

    private void checkValidation_userId(long userId) {
        if (userId < 1) {
            throw new IllegalArgumentException("잘못된 사용자 아이디 입니다.");
        }
    }

    // 사용자의 이전 포인트 조회
    // 아이디에 대한 결과값이 없어도 포인트는 0
    private long getPrePoint(long userId) {
        return userPointTable.selectById(userId).point();
    }

}