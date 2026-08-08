package com.yapp.todakun.notification.adapter.persistence;

import com.yapp.todakun.notification.NotificationDeliveryFailure;
import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.shared.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "notification_delivery_failure",
        // 재시도 폴러가 매분 next_retry_at 기준으로 조회한다.
        indexes = @Index(name = "ix_notification_delivery_failure_next_retry_at", columnList = "next_retry_at")
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationDeliveryFailureJpaEntity extends BaseEntity {

    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType type;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String content;

    @Column(name = "deep_link", updatable = false)
    private String deepLink;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    public static NotificationDeliveryFailureJpaEntity fromDomain(NotificationDeliveryFailure failure) {
        return NotificationDeliveryFailureJpaEntity.builder()
                .id(failure.getId())
                .memberId(failure.getMemberId())
                .notificationId(failure.getNotificationId())
                .type(failure.getType())
                .title(failure.getTitle())
                .content(failure.getContent())
                .deepLink(failure.getDeepLink())
                .attemptCount(failure.getAttemptCount())
                .nextRetryAt(failure.getNextRetryAt())
                .build();
    }

    public NotificationDeliveryFailure toDomain() {
        return NotificationDeliveryFailure.reconstitute(
                getId(),
                memberId,
                notificationId,
                type,
                title,
                content,
                deepLink,
                attemptCount,
                nextRetryAt
        );
    }
}
