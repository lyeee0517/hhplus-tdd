package io.hhplus.tdd.point.model;

public record UserPoint(
        long id,
        long point,
        long updateMillis) {

    //당장 controller이나 dto상에서 유효성 체크까지는 불필요하기 때문에 추가
    public UserPoint {
        if (id < 1) {
            throw new IllegalArgumentException("잘못된 사용자 아이디 입니다.");
        }
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 음수일 수 없습니다.");
        }
    }

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

}
