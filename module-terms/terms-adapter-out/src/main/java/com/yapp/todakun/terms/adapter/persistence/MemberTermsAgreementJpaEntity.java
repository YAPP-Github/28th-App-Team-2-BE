package com.yapp.todakun.terms.adapter.persistence;

import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.terms.MemberTermsAgreement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "member_terms_agreement",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_terms_agreement_member_terms",
                columnNames = {"member_id", "terms_id"}
        ),
        // 알림 발송 대상 조회("특정 약관에 동의한 회원") 최적화 — terms_id 선행 복합 인덱스.
        indexes = @Index(
                name = "ix_member_terms_agreement_terms_agreed",
                columnList = "terms_id, agreed"
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberTermsAgreementJpaEntity extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID memberId;

    @Column(nullable = false, updatable = false)
    private UUID termsId;

    @Column(nullable = false)
    private boolean agreed;

    public static MemberTermsAgreementJpaEntity fromDomain(MemberTermsAgreement agreement) {
        return MemberTermsAgreementJpaEntity.builder()
                .id(agreement.getId())
                .memberId(agreement.getMemberId())
                .termsId(agreement.getTermsId())
                .agreed(agreement.getAgreed())
                .build();
    }

    public MemberTermsAgreement toDomain() {
        return MemberTermsAgreement.reconstitute(
                getId(),
                memberId,
                termsId,
                agreed
        );
    }
}
