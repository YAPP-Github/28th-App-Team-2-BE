package com.yapp.todakun.member.adapter.persistence;

import com.yapp.todakun.member.MemberWithdrawalLog;
import com.yapp.todakun.member.WithdrawalReason;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 회원 탈퇴 사유 로그. 하드 삭제된 회원의 사유·상세만 통계 목적으로 보관한다(회원 FK 없음). */
@Entity
@Table(name = "member_withdrawal_log")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberWithdrawalLogJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalReason reason;

    @Column(length = 500)
    private String detail;

    public static MemberWithdrawalLogJpaEntity fromDomain(MemberWithdrawalLog log) {
        return MemberWithdrawalLogJpaEntity.builder()
                .id(log.getId())
                .memberId(log.getMemberId())
                .reason(log.getReason())
                .detail(log.getDetail())
                .build();
    }

    public MemberWithdrawalLog toDomain() {
        return MemberWithdrawalLog.reconstitute(getId(), memberId, reason, detail);
    }
}
