package com.yapp.todakun.luck.adapter.persistence;

import com.yapp.todakun.luck.LuckAction;
import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.shared.FortuneCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "luck_action",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_luck_action_member_id_fortune_category_fortune_date",
                columnNames = {"member_id", "fortune_category", "fortune_date"}
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LuckActionJpaEntity extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FortuneCategory fortuneCategory;

    @Column(nullable = false, updatable = false)
    private LocalDate fortuneDate;

    @Column(nullable = false, updatable = false)
    private int score;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    // 유일하게 생성 이후에도 바뀌는 필드 — 사용자가 실제 수행 여부를 나중에 체크/취소한다.
    @Column(nullable = false)
    private boolean achieved;

    public static LuckActionJpaEntity fromDomain(LuckAction luckAction) {
        return LuckActionJpaEntity.builder()
                .id(luckAction.getId())
                .memberId(luckAction.getMemberId())
                .fortuneCategory(luckAction.getFortuneCategory())
                .fortuneDate(luckAction.getFortuneDate())
                .score(luckAction.getScore())
                .title(luckAction.getTitle())
                .content(luckAction.getContent())
                .achieved(luckAction.getAchieved())
                .build();
    }

    public LuckAction toDomain() {
        return LuckAction.reconstitute(
                getId(),
                memberId,
                fortuneCategory,
                fortuneDate,
                score,
                title,
                content,
                achieved
        );
    }
}
