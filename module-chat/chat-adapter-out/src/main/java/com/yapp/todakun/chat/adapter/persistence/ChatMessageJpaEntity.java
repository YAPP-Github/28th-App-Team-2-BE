package com.yapp.todakun.chat.adapter.persistence;

import com.yapp.todakun.chat.ChatAction;
import com.yapp.todakun.chat.ChatActionType;
import com.yapp.todakun.chat.ChatMessage;
import com.yapp.todakun.chat.ChatMessageRole;
import com.yapp.todakun.chat.ChatMessageStatus;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 대화 메시지. 액션 카드는 jsonb 없이 nullable 컬럼 4개로 펼쳐 저장한다(있을 때만 4개 모두 채워진다). */
@Entity
@Table(
        name = "chat_message",
        indexes = @Index(name = "ix_chat_message_conversation", columnList = "conversation_id, created_at")
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatMessageJpaEntity extends BaseEntity {

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ChatMessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageStatus status;

    @Enumerated(EnumType.STRING)
    private ChatActionType actionType;

    private String actionLabel;

    private String actionCategory;

    private LocalDate actionDate;

    public static ChatMessageJpaEntity fromDomain(ChatMessage message) {
        ChatAction action = message.getAction();

        return ChatMessageJpaEntity.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .role(message.getRole())
                .content(message.getContent())
                .status(message.getStatus())
                .actionType(action != null ? action.getType() : null)
                .actionLabel(action != null ? action.getLabel() : null)
                .actionCategory(action != null ? action.getCategory() : null)
                .actionDate(action != null ? action.getDate() : null)
                .build();
    }

    public ChatMessage toDomain() {
        boolean hasAction = actionType != null && actionLabel != null && actionCategory != null && actionDate != null;
        ChatAction action = hasAction
                ? new ChatAction(actionType, actionLabel, actionCategory, actionDate)
                : null;

        return ChatMessage.reconstitute(getId(), conversationId, role, content, status, action, getCreatedAt());
    }
}
