package com.yapp.todakun.notification.adapter.persistence;

import com.yapp.todakun.notification.NotificationSetting;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "notification_setting",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_setting_member",
                columnNames = "member_id"
        ),
        // 아침 운 리포트 스케줄러의 대상 조회(활성 + 받을 시간) 최적화.
        indexes = @Index(
                name = "ix_notification_setting_morning",
                columnList = "morning_report_enabled, morning_report_time"
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationSettingJpaEntity extends BaseEntity {

    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @Column(name = "morning_report_enabled", nullable = false)
    private boolean morningReportEnabled;

    @Column(name = "morning_report_time", nullable = false)
    private LocalTime morningReportTime;

    @Column(name = "todaki_enabled", nullable = false)
    private boolean todakiEnabled;

    @Column(name = "lucky_action_reminder_enabled", nullable = false)
    private boolean luckyActionReminderEnabled;

    public static NotificationSettingJpaEntity fromDomain(NotificationSetting setting) {
        return NotificationSettingJpaEntity.builder()
                .id(setting.getId())
                .memberId(setting.getMemberId())
                .morningReportEnabled(setting.getMorningReportEnabled())
                .morningReportTime(setting.getMorningReportTime())
                .todakiEnabled(setting.getTodakiEnabled())
                .luckyActionReminderEnabled(setting.getLuckyActionReminderEnabled())
                .build();
    }

    public NotificationSetting toDomain() {
        return NotificationSetting.reconstitute(
                getId(),
                memberId,
                morningReportEnabled,
                morningReportTime,
                todakiEnabled,
                luckyActionReminderEnabled
        );
    }
}
