package com.yapp.todakun.chat.adapter.persistence;

import com.yapp.todakun.chat.ChatConversation;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 대화 스레드. [lastMessageAt]은 JPA 감사(updatedAt)에 맡기지 않고 애플리케이션이 명시적으로 갱신하는 필드다(도메인 주석 참고). */
@Entity
@Table(
        name = "chat_conversation",
        indexes = @Index(name = "ix_chat_conversation_member", columnList = "member_id, last_message_at DESC")
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatConversationJpaEntity extends BaseEntity {

    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean unread;

    @Column(nullable = false)
    private Instant lastMessageAt;

    public static ChatConversationJpaEntity fromDomain(ChatConversation conversation) {
        return ChatConversationJpaEntity.builder()
                .id(conversation.getId())
                .memberId(conversation.getMemberId())
                .title(conversation.getTitle())
                .unread(conversation.getUnread())
                .lastMessageAt(conversation.getLastMessageAt())
                .build();
    }

    public ChatConversation toDomain() {
        return ChatConversation.reconstitute(getId(), memberId, title, unread, lastMessageAt, getCreatedAt());
    }
}
