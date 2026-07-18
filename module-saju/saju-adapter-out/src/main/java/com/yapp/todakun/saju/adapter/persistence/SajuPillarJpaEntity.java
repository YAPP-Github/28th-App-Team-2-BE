package com.yapp.todakun.saju.adapter.persistence;

import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.saju.EarthlyBranch;
import com.yapp.todakun.saju.HeavenlyStem;
import com.yapp.todakun.saju.PillarType;
import com.yapp.todakun.saju.SajuPillar;
import com.yapp.todakun.saju.Sibiunseong;
import com.yapp.todakun.saju.Sipseong;
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

/** 4주별 상세(천간/지지/십성/십이운성). 천간·지지는 라이브러리 코드값, 십성/십이운성은 도메인 enum. */
@Entity
@Table(name = "saju_pillar")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SajuPillarJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID chartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PillarType pillarType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HeavenlyStem heavenlyStem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EarthlyBranch earthlyBranch;

    @Enumerated(EnumType.STRING)
    @Column
    private Sipseong cheonganSipseong;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sipseong jijiSipseong;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sibiunseong sibiunseong;

    public static SajuPillarJpaEntity fromDomain(UUID id, UUID chartId, SajuPillar pillar) {
        return SajuPillarJpaEntity.builder()
                .id(id)
                .chartId(chartId)
                .pillarType(pillar.getPillarType())
                .heavenlyStem(pillar.getStem())
                .earthlyBranch(pillar.getBranch())
                .cheonganSipseong(pillar.getStemSipseong())
                .jijiSipseong(pillar.getBranchSipseong())
                .sibiunseong(pillar.getSibiunseong())
                .build();
    }

    public SajuPillar toDomain() {
        return new SajuPillar(pillarType, heavenlyStem, earthlyBranch, cheonganSipseong, jijiSipseong, sibiunseong);
    }
}
