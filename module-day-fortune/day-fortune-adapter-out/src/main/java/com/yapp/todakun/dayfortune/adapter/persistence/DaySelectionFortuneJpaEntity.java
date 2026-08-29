package com.yapp.todakun.dayfortune.adapter.persistence;

import com.yapp.todakun.dayfortune.DaySelectionFortune;
import com.yapp.todakun.dayfortune.DaySelectionPurpose;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "day_selection_fortune",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_day_selection_fortune_member_id_purpose_target_date",
                columnNames = {"member_id", "purpose", "target_date"}
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DaySelectionFortuneJpaEntity extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private DaySelectionPurpose purpose;

    @Column(nullable = false, updatable = false)
    private LocalDate targetDate;

    @Column(nullable = false, updatable = false)
    private int score;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    @CollectionTable(
            name = "day_selection_fortune_category",
            joinColumns = @JoinColumn(
                    name = "day_selection_fortune_id",
                    nullable = false,
                    foreignKey = @ForeignKey(name = "fk_day_selection_fortune_category_day_selection_fortune_id")
            )
    )
    @OrderColumn(name = "category_order")
    private List<FortuneCategoryStarEmbeddable> fortuneCategories;

    public static DaySelectionFortuneJpaEntity fromDomain(DaySelectionFortune daySelectionFortune) {
        return DaySelectionFortuneJpaEntity.builder()
                .id(daySelectionFortune.getId())
                .memberId(daySelectionFortune.getMemberId())
                .purpose(daySelectionFortune.getPurpose())
                .targetDate(daySelectionFortune.getTargetDate())
                .score(daySelectionFortune.getScore())
                .title(daySelectionFortune.getTitle())
                .content(daySelectionFortune.getContent())
                .fortuneCategories(mapToList(daySelectionFortune.getFortuneCategories(), FortuneCategoryStarEmbeddable::fromDomain))
                .build();
    }

    public DaySelectionFortune toDomain() {
        return DaySelectionFortune.reconstitute(
                getId(),
                memberId,
                purpose,
                targetDate,
                score,
                title,
                content,
                // @ElementCollection(LAZY)인 PersistentList를 도메인에 그대로 넘기면 세션 종료(OSIV off) 후
                // 접근 시 LazyInitializationException이 난다. 트랜잭션 안에서 새 리스트로 매핑해 프록시 분리.
                mapToList(fortuneCategories, FortuneCategoryStarEmbeddable::toDomain)
        );
    }

    private static <T, R> List<R> mapToList(
            List<T> source,
            Function<T, R> mapper
    ) {
        return source.stream().map(mapper).collect(Collectors.toList());
    }
}
