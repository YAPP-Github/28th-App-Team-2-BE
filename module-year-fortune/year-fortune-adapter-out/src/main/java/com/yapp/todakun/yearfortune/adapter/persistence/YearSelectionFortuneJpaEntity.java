package com.yapp.todakun.yearfortune.adapter.persistence;

import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.yearfortune.YearSelectionFortune;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "year_selection_fortune",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_year_selection_fortune_member_id_year",
                columnNames = {"member_id", "year"}
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class YearSelectionFortuneJpaEntity extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID memberId;

    @Column(nullable = false, updatable = false)
    private int year;

    @Column(nullable = false, updatable = false)
    private int score;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    @CollectionTable(
            name = "year_selection_fortune_category",
            joinColumns = @JoinColumn(
                    name = "year_selection_fortune_id",
                    nullable = false,
                    foreignKey = @ForeignKey(name = "fk_year_selection_fortune_category_year_selection_fortune_id")
            )
    )
    @OrderColumn(name = "category_order")
    private List<FortuneCategoryStarEmbeddable> fortuneCategories;

    public static YearSelectionFortuneJpaEntity fromDomain(YearSelectionFortune yearSelectionFortune) {
        return YearSelectionFortuneJpaEntity.builder()
                .id(yearSelectionFortune.getId())
                .memberId(yearSelectionFortune.getMemberId())
                .year(yearSelectionFortune.getYear())
                .score(yearSelectionFortune.getScore())
                .title(yearSelectionFortune.getTitle())
                .content(yearSelectionFortune.getContent())
                .fortuneCategories(mapToList(yearSelectionFortune.getFortuneCategories(), FortuneCategoryStarEmbeddable::fromDomain))
                .build();
    }

    public YearSelectionFortune toDomain() {
        return YearSelectionFortune.reconstitute(
                getId(),
                memberId,
                year,
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
