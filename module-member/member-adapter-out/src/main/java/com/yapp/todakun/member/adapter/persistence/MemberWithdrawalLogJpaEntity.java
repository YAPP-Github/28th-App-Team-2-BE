package com.yapp.todakun.member.adapter.persistence;

import com.yapp.todakun.member.MemberWithdrawalLog;
import com.yapp.todakun.member.WithdrawalReason;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 회원 탈퇴 사유 로그. 탈퇴 정책 v1.0(4-3)에 따라 계정 식별 정보와 분리한 비식별 기록으로,
 * 사유·상세만 통계 목적으로 보관한다(회원 식별자 미보관).
 */
@Entity
@Table(name = "member_withdrawal_log")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberWithdrawalLogJpaEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalReason reason;

    @Column(length = 200)
    private String detail;

    public static MemberWithdrawalLogJpaEntity fromDomain(MemberWithdrawalLog log) {
        return MemberWithdrawalLogJpaEntity.builder()
                .id(log.getId())
                .reason(log.getReason())
                .detail(log.getDetail())
                .build();
    }

    public MemberWithdrawalLog toDomain() {
        return MemberWithdrawalLog.reconstitute(getId(), reason, detail);
    }
}
