package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.model.UserPoint;

public interface PointService {

    UserPoint chargePoint(long userId, long amount);

    UserPoint usePoint(long userId, long amount);

    UserPoint selectPointById(long userId);
    
}