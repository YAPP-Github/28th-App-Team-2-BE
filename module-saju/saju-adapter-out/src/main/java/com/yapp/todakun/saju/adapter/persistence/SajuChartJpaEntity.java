package com.yapp.todakun.saju.adapter.persistence;

import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.saju.BirthTime;
import com.yapp.todakun.saju.CalendarType;
import com.yapp.todakun.saju.Gender;
import com.yapp.todakun.saju.HeavenlyStem;
import com.yapp.todakun.saju.OhaengCount;
import com.yapp.todakun.saju.SajuChart;
import com.yapp.todakun.saju.SajuPillar;
import com.yapp.todakun.saju.SipseongCount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 사주 명식 계산 결과 헤더(순수 계산 결과만 보유). 소유권(본인/상대)은 member_saju_chart가 관리한다. */
@Entity
@Table(name = "saju_chart")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SajuChartJpaEntity extends BaseEntity {

    @Column
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalendarType calendarType;

    @Column(nullable = false)
    private LocalDate inputDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BirthTime birthTime;

    @Column(nullable = false)
    private boolean isLeapMonth;

    @Column(nullable = false)
    private boolean isTimeUnknown;

    @Column
    private String solarTermName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HeavenlyStem dayMasterStem;

    public static SajuChartJpaEntity fromDomain(SajuChart chart) {
        return SajuChartJpaEntity.builder()
                .id(chart.getId())
                .name(chart.getName())
                .gender(chart.getGender())
                .calendarType(chart.getCalendarType())
                .inputDate(chart.getInputDate())
                .birthTime(chart.getBirthTime())
                .isLeapMonth(chart.isLeapMonth())
                .isTimeUnknown(chart.isTimeUnknown())
                .solarTermName(chart.getSolarTermName())
                .dayMasterStem(chart.getDayMaster())
                .build();
    }

    public SajuChart toDomain(List<SajuPillar> pillars, List<OhaengCount> ohaeng, List<SipseongCount> sipseong) {
        return SajuChart.reconstitute(
                getId(),
                name,
                gender,
                calendarType,
                inputDate,
                birthTime,
                isLeapMonth,
                isTimeUnknown,
                solarTermName,
                dayMasterStem,
                pillars,
                ohaeng,
                sipseong
        );
    }
}
