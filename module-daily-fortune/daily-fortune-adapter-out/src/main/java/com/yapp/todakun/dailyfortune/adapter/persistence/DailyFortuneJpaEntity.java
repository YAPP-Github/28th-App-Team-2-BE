package com.yapp.todakun.dailyfortune.adapter.persistence;

import com.yapp.todakun.dailyfortune.DailyFortune;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "daily_fortune",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_fortune_member_id_fortune_date",
                columnNames = {"member_id", "fortune_date"}
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DailyFortuneJpaEntity extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID memberId;

    @Column(nullable = false, updatable = false)
    private LocalDate fortuneDate;

    @Column(nullable = false, updatable = false)
    private int score;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    @CollectionTable(
            name = "daily_fortune_lucky_item",
            joinColumns = @JoinColumn(
                    name = "daily_fortune_id",
                    nullable = false,
                    foreignKey = @ForeignKey(name = "fk_daily_fortune_lucky_item_daily_fortune_id")
            )
    )
    @OrderColumn(name = "item_order")
    @Column(nullable = false)
    private List<String> luckyItems;

    @ElementCollection
    @CollectionTable(
            name = "daily_fortune_cautionary_item",
            joinColumns = @JoinColumn(
                    name = "daily_fortune_id",
                    nullable = false,
                    foreignKey = @ForeignKey(name = "fk_daily_fortune_cautionary_item_daily_fortune_id")
            )
    )
    @OrderColumn(name = "item_order")
    @Column(nullable = false)
    private List<String> cautionaryItems;

    public static DailyFortuneJpaEntity fromDomain(DailyFortune dailyFortune) {
        return DailyFortuneJpaEntity.builder()
                .id(dailyFortune.getId())
                .memberId(dailyFortune.getMemberId())
                .fortuneDate(dailyFortune.getFortuneDate())
                .score(dailyFortune.getScore())
                .title(dailyFortune.getTitle())
                .content(dailyFortune.getContent())
                .luckyItems(dailyFortune.getLuckyItems())
                .cautionaryItems(dailyFortune.getCautionaryItems())
                .build();
    }

    public DailyFortune toDomain() {
        return DailyFortune.reconstitute(
                getId(),
                memberId,
                fortuneDate,
                score,
                title,
                content,
                // @ElementCollection(LAZY)인 PersistentList를 도메인에 그대로 넘기면 세션 종료(OSIV off) 후
                // 접근 시 LazyInitializationException이 난다. 트랜잭션 안에서 방어적 복사로 초기화 + 프록시 분리.
                new ArrayList<>(luckyItems),
                new ArrayList<>(cautionaryItems)
        );
    }
}
