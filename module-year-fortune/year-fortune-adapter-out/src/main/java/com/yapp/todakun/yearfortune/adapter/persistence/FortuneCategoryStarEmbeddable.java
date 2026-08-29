package com.yapp.todakun.yearfortune.adapter.persistence;

import com.yapp.todakun.shared.FortuneCategory;
import com.yapp.todakun.yearfortune.FortuneCategoryStar;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/** 연도별 운세의 운세 카테고리별 별점을 저장하는 임베더블. 독립된 식별자 없이 YearSelectionFortuneJpaEntity에 종속된다. */
@Embeddable
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FortuneCategoryStarEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FortuneCategory fortuneCategory;

    @Column(nullable = false)
    private int star;

    public static FortuneCategoryStarEmbeddable fromDomain(FortuneCategoryStar fortuneCategoryStar) {
        return FortuneCategoryStarEmbeddable.builder()
                .fortuneCategory(fortuneCategoryStar.getFortuneCategory())
                .star(fortuneCategoryStar.getStar())
                .build();
    }

    public FortuneCategoryStar toDomain() {
        return new FortuneCategoryStar(fortuneCategory, star);
    }
}
