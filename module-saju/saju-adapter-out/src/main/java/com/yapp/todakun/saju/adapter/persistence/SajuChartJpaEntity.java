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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 사주 명식 계산 결과 헤더. 본인/타인(궁합 상대 등) 입력을 모두 이 테이블로 처리한다(is_self로 구분). */
@Entity
@Table(name = "saju_chart")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SajuChartJpaEntity extends BaseEntity {

    @Column
    private UUID userId;

    @Column(nullable = false)
    private boolean isSelf;

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
                .userId(chart.getUserId())
                .isSelf(chart.isSelf())
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
                userId,
                isSelf,
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
