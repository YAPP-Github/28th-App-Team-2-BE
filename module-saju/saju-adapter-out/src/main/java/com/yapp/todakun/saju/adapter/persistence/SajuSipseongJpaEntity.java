package com.yapp.todakun.saju.adapter.persistence;

import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.saju.Sipseong;
import com.yapp.todakun.saju.SipseongCount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 십성 분포 집계(항상 10행, 0건 포함). */
@Entity
@Table(
        name = "saju_sipseong",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_saju_sipseong_chart_type",
                columnNames = {"chart_id", "sipseong_type"}
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SajuSipseongJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID chartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sipseong sipseongType;

    @Column(nullable = false)
    private int count;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    public static SajuSipseongJpaEntity fromDomain(UUID id, UUID chartId, SipseongCount sipseong) {
        return SajuSipseongJpaEntity.builder()
                .id(id)
                .chartId(chartId)
                .sipseongType(sipseong.getSipseong())
                .count(sipseong.getCount())
                .percentage(BigDecimal.valueOf(sipseong.getPercentage()))
                .build();
    }

    public SipseongCount toDomain() {
        return new SipseongCount(sipseongType, count, percentage.doubleValue());
    }
}
