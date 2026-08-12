package com.yapp.todakun.notification.adapter.persistence;

import com.yapp.todakun.notification.NoticeDispatchHistory;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "notice_dispatch_history",
        // 선조회를 통과한 동시 실행이 동시에 INSERT하는 경우까지 막는 최후 방어선(중복 발송 차단).
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notice_dispatch_history_idempotency_key",
                columnNames = "idempotency_key"
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NoticeDispatchHistoryJpaEntity extends BaseEntity {

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String content;

    @Column(name = "deep_link", updatable = false)
    private String deepLink;

    @Column(name = "dispatched_at", nullable = false, updatable = false)
    private Instant dispatchedAt;

    public static NoticeDispatchHistoryJpaEntity fromDomain(NoticeDispatchHistory history) {
        return NoticeDispatchHistoryJpaEntity.builder()
                .id(history.getId())
                .idempotencyKey(history.getIdempotencyKey())
                .title(history.getTitle())
                .content(history.getContent())
                .deepLink(history.getDeepLink())
                .dispatchedAt(history.getDispatchedAt())
                .build();
    }

    public NoticeDispatchHistory toDomain() {
        return NoticeDispatchHistory.reconstitute(getId(), idempotencyKey, title, content, deepLink, dispatchedAt);
    }
}
