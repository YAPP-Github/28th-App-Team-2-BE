package com.yapp.todakun.compatibility.adapter.persistence;

import com.yapp.todakun.compatibility.CompatibilityRelationshipType;
import com.yapp.todakun.compatibility.SajuCompatibility;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 두 명식을 조합한 궁합 결과 헤더. (member_id, my_chart_id, partner_chart_id) 조합은 유니크(멱등).
 * 오행 시각화는 saju_compatibility_ohaeng 컬렉션 테이블에 종속 저장한다.
 */
@Entity
@Table(
        name = "saju_compatibility",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_saju_compatibility_member_charts",
                columnNames = {"member_id", "my_chart_id", "partner_chart_id"}
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SajuCompatibilityJpaEntity extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID memberId;

    @Column(nullable = false, updatable = false)
    private UUID myChartId;

    @Column(nullable = false, updatable = false)
    private UUID partnerChartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CompatibilityRelationshipType relationshipType;

    @Column(nullable = false, updatable = false)
    private int score;

    @Column(nullable = false, updatable = false)
    private String headline;

    @Column(nullable = false, updatable = false)
    private String subheadline;

    @Column(nullable = false, updatable = false)
    private String summary;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String totalAnalysis;

    @Column(nullable = false, updatable = false)
    private String analysisBasis;

    @ElementCollection
    @CollectionTable(
            name = "saju_compatibility_ohaeng",
            joinColumns = @JoinColumn(
                    name = "saju_compatibility_id",
                    nullable = false,
                    foreignKey = @ForeignKey(name = "fk_saju_compatibility_ohaeng_saju_compatibility_id")
            )
    )
    @OrderColumn(name = "ohaeng_order")
    private List<CompatibilityOhaengEmbeddable> ohaengs;

    public static SajuCompatibilityJpaEntity fromDomain(SajuCompatibility compatibility) {
        return SajuCompatibilityJpaEntity.builder()
                .id(compatibility.getId())
                .memberId(compatibility.getMemberId())
                .myChartId(compatibility.getMyChartId())
                .partnerChartId(compatibility.getPartnerChartId())
                .relationshipType(compatibility.getRelationshipType())
                .score(compatibility.getScore())
                .headline(compatibility.getHeadline())
                .subheadline(compatibility.getSubheadline())
                .summary(compatibility.getSummary())
                .totalAnalysis(compatibility.getTotalAnalysis())
                .analysisBasis(compatibility.getAnalysisBasis())
                .ohaengs(mapToList(compatibility.getOhaengs(), CompatibilityOhaengEmbeddable::fromDomain))
                .build();
    }

    public SajuCompatibility toDomain() {
        return SajuCompatibility.reconstitute(
                getId(),
                memberId,
                myChartId,
                partnerChartId,
                relationshipType,
                score,
                headline,
                subheadline,
                summary,
                totalAnalysis,
                analysisBasis,
                // @ElementCollection(LAZY)인 PersistentList를 도메인에 그대로 넘기면 세션 종료(OSIV off) 후
                // 접근 시 LazyInitializationException이 난다. 트랜잭션 안에서 새 리스트로 매핑해 프록시 분리.
                mapToList(ohaengs, CompatibilityOhaengEmbeddable::toDomain)
        );
    }

    private static <T, R> List<R> mapToList(
            List<T> source,
            Function<T, R> mapper
    ) {
        return source.stream().map(mapper).collect(Collectors.toList());
    }
}
