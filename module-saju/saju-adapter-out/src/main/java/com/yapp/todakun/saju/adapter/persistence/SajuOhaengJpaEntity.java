package com.yapp.todakun.saju.adapter.persistence;

import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.saju.Element;
import com.yapp.todakun.saju.OhaengCount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 오행 분포 집계(항상 5행). */
@Entity
@Table(name = "saju_ohaeng")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SajuOhaengJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID chartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Element element;

    @Column(nullable = false)
    private int count;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    public static SajuOhaengJpaEntity fromDomain(UUID id, UUID chartId, OhaengCount ohaeng) {
        return SajuOhaengJpaEntity.builder()
                .id(id)
                .chartId(chartId)
                .element(ohaeng.getElement())
                .count(ohaeng.getCount())
                .percentage(BigDecimal.valueOf(ohaeng.getPercentage()))
                .build();
    }

    public OhaengCount toDomain() {
        return new OhaengCount(element, count, percentage.doubleValue());
    }
}
