package io.hhplus.tdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.displayName;
import io.hhplus.tdd.point.database.PointHistoryTable;
import io.hhplus.tdd.point.database.UserPointTable;
import io.hhplus.tdd.point.model.TransactionType;
import io.hhplus.tdd.point.model.UserPoint;
import io.hhplus.tdd.point.service.PointServiceImpl;

@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

    @InjectMocks
    private PointServiceImpl pointService;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Nested
    @DisplayName("포인트 충전 테스트")
    class ChargeTest {

        /*
         * 실패 테스트 시 유효성 검사 조건을 어느정도로 주고 해야하는지 의문
         * 현재 조건이 userId < 1 이므로 음수일 때랑 0일때 다 해야하는지
         * 사실 userId 필수 확인도 해야하지만 보통 그것은 controller단에서 잡기 때문에
         * 현재는 service 중심 테스트로 진행하였음
         */
        @Test
        @DisplayName("포인트 충전 테스트 - 실패 : 사용자 아이디 검증 실패")
        void chargePointTest_validation_fail_ByUserId() {
            // given
            long userId = 0; // 잘못된 사용자 아이디
            long amount = 1000L;

            // when
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                pointService.chargePoint(userId, amount);
            });

            // then
            assertThat(exception.getMessage()).isEqualTo("잘못된 사용자 아이디 입니다.");
        }

        @Test
        @DisplayName("포인트 충전 테스트 - 실패 : 충전 포인트 검증 실패(음수 불가)")
        void chargePointTest_validation_fail_ByPointAmount_is_More0() {
            // given
            long userId = 1L;
            long amount = -50;

            // when
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                pointService.chargePoint(userId, amount);
            });

            // then
            assertThat(exception.getMessage()).isEqualTo("충전/사용 금액은 1000원부터 가능합니다.");
        }

        @Test
        @DisplayName("포인트 충전 테스트 - 실패 : 충전 포인트 검증 실패(최소 충전 금액 1000원)")
        void chargePointTest_validation_fail_ByPointAmount_is_Unit10() {
            // given
            long userId = 1L;
            long amount = 990L;

            // when
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                pointService.chargePoint(userId, amount);
            });

            // then
            assertThat(exception.getMessage()).isEqualTo("충전/사용 금액은 1000원부터 가능합니다.");
        }

        @Test
        @displayName("포인트 충전 시 이전 포인트 조회 로직 동작하는지 확인")
        void chargePointTest_findPrePoint_logic_MostOne() {
            // given
            long userId = 1L;
            long amount = 1000L;

            when(userPointTable.selectById(1L)).thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));
            when(userPointTable.insertOrUpdate(userId, 2000L))
                    .thenReturn(new UserPoint(userId, 2000L, System.currentTimeMillis()));

            // when
            pointService.chargePoint(userId, amount);

            // then
            verify(userPointTable, times(1)).selectById(userId);
        }

        @Test
        @DisplayName("포인트 충전 시 이전까지 포인트가 0일 때 충전금액 = 저장된 총 포인트 금액")
        void chargePointTest_Calculation_TotalPoint_Is_newPoint_byPrePoint_is_0() {
            // given
            long userId = 1L;
            long amount = 1000L;

            // 임의 이전 데이터 세팅
            UserPoint preInfo = new UserPoint(userId, 0L, System.currentTimeMillis());

            when(userPointTable.selectById(1L)).thenReturn(preInfo);

            long totalPointValue = preInfo.point() + amount;
            UserPoint ExpectChargeInfo = new UserPoint(userId, totalPointValue, System.currentTimeMillis());

            when(userPointTable.insertOrUpdate(userId, totalPointValue)).thenReturn(ExpectChargeInfo);

            // when
            UserPoint result = pointService.chargePoint(userId, amount);

            // then
            assertThat(result.point()).isEqualTo(amount);
        }

        @Test
        @DisplayName("포인트 충전 시 충전되는 포인트 = 사용자 아이디에 해당하는 이전 포인트 + 새로운 포인트 금액")
        void chargePointTest_Calculation_TotalPoint_Is_prePoint_Plus_newPoint() {
            // given
            long userId = 1L;
            long amount = 1000L;

            // 임의 이전 데이터 세팅
            UserPoint preInfo = new UserPoint(userId, 3000L, System.currentTimeMillis());
            when(userPointTable.selectById(1L)).thenReturn(preInfo);

            long totalPointValue = preInfo.point() + amount;

            UserPoint ExpectChargeInfo = new UserPoint(userId, totalPointValue, System.currentTimeMillis());
            when(userPointTable.insertOrUpdate(userId, totalPointValue)).thenReturn(ExpectChargeInfo);

            // when
            UserPoint result = pointService.chargePoint(userId, amount);

            // then
            assertThat(result.point()).isEqualTo(4000L);
        }

        @Test
        @DisplayName("포인트 충전 시 포인트 히스토리 동작하는 지 확인")
        void chargePointTest_SavePointHistory_MostOne() {
            // given
            long userId = 1L;
            long amount = 1000L;

            // 임의 이전 데이터 세팅
            when(userPointTable.selectById(1L)).thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));
            when(userPointTable.insertOrUpdate(userId, 2000L))
                    .thenReturn(new UserPoint(userId, 2000L, System.currentTimeMillis()));

            // when
            pointService.chargePoint(userId, amount);

            // then
            verify(pointHistoryTable, times(1)).insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
        }

        @Test
        @displayName("포인트 충전 시 포인트 히스토리 옳바른 저장 데이터 확인")
        void chargePointTest_SavePointHistory_CorrectValues() {
            // given
            long userId = 1L;
            long amount = 1000L;

            when(userPointTable.selectById(userId))
                    .thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));
            when(userPointTable.insertOrUpdate(userId, 2000L))
                    .thenReturn(new UserPoint(userId, 2000L, System.currentTimeMillis()));

            // when
            pointService.chargePoint(userId, amount);

            // then
            ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<TransactionType> typeCaptor = ArgumentCaptor.forClass(TransactionType.class);

            verify(pointHistoryTable, times(1))
                    .insert(userIdCaptor.capture(), amountCaptor.capture(), typeCaptor.capture(),
                            anyLong());

            // 값 검증
            assertThat(userIdCaptor.getValue()).isEqualTo(userId);
            assertThat(amountCaptor.getValue()).isEqualTo(amount);
            assertThat(typeCaptor.getValue()).isEqualTo(TransactionType.CHARGE);
        }
    }

    @Nested
    @displayName("포인트 사용 테스트")
    class useTest {

        @Test
        @DisplayName("포인트 사용 테스트 - 실패 : 사용자 아이디 검증 실패")
        void usePointTest_validation_fail_ByUserId() {
            // given
            long userId = 0; // 잘못된 사용자 아이디
            long amount = 1000L;

            // when
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                pointService.usePoint(userId, amount);
            });

            // then
            assertThat(exception.getMessage()).isEqualTo("잘못된 사용자 아이디 입니다.");
        }

        @Test
        @DisplayName("포인트 사용 테스트 - 실패 : 사용 포인트 검증 실패(음수 불가)")
        void usePointTest_validation_fail_ByPointAmount_is_More0() {
            // given
            long userId = 1L;
            long amount = -50;

            // when
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                pointService.usePoint(userId, amount);
            });

            // then
            assertThat(exception.getMessage()).isEqualTo("충전/사용 금액은 1000원부터 가능합니다.");
        }

        @Test
        @displayName("포인트 사용 시 이전 포인트 조회 로직 동작하는지 확인")
        void usePointTest_findPrePoint_logic_MostOne() {
            // given
            long userId = 1L;
            long amount = 1000L;

            when(userPointTable.selectById(1L)).thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));
            when(userPointTable.insertOrUpdate(userId, 0))
                    .thenReturn(new UserPoint(userId, 0, System.currentTimeMillis()));

            // when
            pointService.usePoint(userId, amount);

            // then
            verify(userPointTable, times(1)).selectById(userId);

        }

        @Test
        @DisplayName("포인트 사용 테스트 - 실패 : 사용하려는 포인트가 이전 포인트보다 많으면 안됨")
        void usePointTest_validation_fail_ByPointAmount_is_NoMoreThen_PrePoint() {
            // given
            long userId = 1L;
            long amount = 5000L;

            UserPoint preInfo = new UserPoint(userId, 3000L, System.currentTimeMillis());
            when(userPointTable.selectById(1L)).thenReturn(preInfo);

            // when
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                pointService.usePoint(userId, amount);
            });

            // then
            assertThat(exception.getMessage()).isEqualTo("포인트가 부족합니다.");
        }

        @Test
        @DisplayName("포인트 사용 시 저장 포인트 = 사용자 아이디에 해당하는 이전 포인트 - 사용하려는 포인트 금액")
        void usePointTest_Calculation_TotalPoint_Is_prePoint_Minus_newPoint() {
            // given
            long userId = 1L;
            long amount = 1000L;

            // 임의 이전 데이터 세팅
            UserPoint preInfo = new UserPoint(userId, 1000L, System.currentTimeMillis());
            when(userPointTable.selectById(1L)).thenReturn(preInfo);

            long totalPointValue = preInfo.point() - amount;

            UserPoint ExpectUseInfo = new UserPoint(userId, totalPointValue, System.currentTimeMillis());
            when(userPointTable.insertOrUpdate(userId, totalPointValue)).thenReturn(ExpectUseInfo);

            // when
            UserPoint result = pointService.usePoint(userId, amount);

            // then
            assertThat(result.point()).isEqualTo(0);
        }

        @Test
        @DisplayName("포인트 사용 시 포인트 히스토리 동작하는 지 확인")
        void usePointTest_SavePointHistory_MostOne() {
            // given
            long userId = 1L;
            long amount = 1000L;

            // 임의 이전 데이터 세팅
            when(userPointTable.selectById(1L)).thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));
            when(userPointTable.insertOrUpdate(userId, 0))
                    .thenReturn(new UserPoint(userId, 0, System.currentTimeMillis()));

            // when
            pointService.usePoint(userId, amount);

            // then
            verify(pointHistoryTable, times(1)).insert(eq(userId), eq(amount), eq(TransactionType.USE), anyLong());
        }

        @Test
        @displayName("포인트 사용 시 포인트 히스토리 옳바른 저장 데이터 확인")
        void usePointTest_SavePointHistory_CorrectValues() {
            // given
            long userId = 1L;
            long amount = 1000L;

            when(userPointTable.selectById(userId))
                    .thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));
            when(userPointTable.insertOrUpdate(userId, 0))
                    .thenReturn(new UserPoint(userId, 0, System.currentTimeMillis()));

            // when
            pointService.usePoint(userId, amount);

            // then
            ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<TransactionType> typeCaptor = ArgumentCaptor.forClass(TransactionType.class);

            verify(pointHistoryTable, times(1))
                    .insert(userIdCaptor.capture(), amountCaptor.capture(), typeCaptor.capture(),
                            anyLong());

            // 값 검증
            assertThat(userIdCaptor.getValue()).isEqualTo(userId);
            assertThat(amountCaptor.getValue()).isEqualTo(amount);
            assertThat(typeCaptor.getValue()).isEqualTo(TransactionType.USE);
        }

    }

}
