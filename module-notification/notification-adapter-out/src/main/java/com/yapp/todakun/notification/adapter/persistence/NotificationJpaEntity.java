package com.yapp.todakun.notification.adapter.persistence;

import com.yapp.todakun.notification.Notification;
import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.shared.NotificationType;
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

@Entity
@Table(
        name = "notification",
        indexes = {
                // 알림함 목록 조회(회원별 최신순)
                @Index(name = "ix_notification_member_created", columnList = "member_id, created_at"),
                // 안읽음 개수 조회
                @Index(name = "ix_notification_member_read", columnList = "member_id, is_read")
        }
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationJpaEntity extends BaseEntity {

    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType type;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String content;

    @Column(name = "deep_link", updatable = false)
    private String deepLink;

    // 도메인 필드명은 isRead지만, JPA 속성명 'is' 접두사 모호성을 피하려 필드는 read + 컬럼 is_read로 둔다.
    @Column(name = "is_read", nullable = false)
    private boolean read;

    public static NotificationJpaEntity fromDomain(Notification notification) {
        return NotificationJpaEntity.builder()
                .id(notification.getId())
                .memberId(notification.getMemberId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .deepLink(notification.getDeepLink())
                .read(notification.isRead())
                .build();
    }

    public Notification toDomain() {
        return Notification.reconstitute(
                getId(),
                memberId,
                type,
                title,
                content,
                deepLink,
                read,
                getCreatedAt()
        );
    }
}
