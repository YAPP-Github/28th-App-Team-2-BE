package com.yapp.todakun.saju.adapter.persistence;

import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.saju.MemberSajuLink;
import com.yapp.todakun.saju.RelationshipType;
import com.yapp.todakun.saju.SajuRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 회원과 사주 명식의 소유권 연결. role=SELF는 계정 본인 사주, role=PARTNER는 저장해 둔 타인 사주.
 * 명식(saju_chart)과 소유권을 분리해 향후 궁합 등 확장에 대비한다(ERD member_saju_chart).
 */
@Entity
@Table(
        name = "member_saju_chart",
        indexes = {
                @Index(name = "idx_member_saju_chart_member_id_role", columnList = "member_id, role"),
                @Index(name = "idx_member_saju_chart_chart_id", columnList = "chart_id")
        }
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberSajuChartJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private UUID chartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SajuRole role;

    @Enumerated(EnumType.STRING)
    @Column
    private RelationshipType relationshipType;

    public static MemberSajuChartJpaEntity fromDomain(MemberSajuLink link) {
        return MemberSajuChartJpaEntity.builder()
                .id(link.getId())
                .memberId(link.getMemberId())
                .chartId(link.getChartId())
                .role(link.getRole())
                .relationshipType(link.getRelationshipType())
                .build();
    }

    public MemberSajuLink toDomain() {
        return MemberSajuLink.reconstitute(getId(), memberId, chartId, role, relationshipType);
    }
}
