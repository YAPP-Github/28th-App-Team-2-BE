package com.yapp.todakun.compatibility.adapter.persistence;

import com.yapp.todakun.compatibility.CompatibilityElement;
import com.yapp.todakun.compatibility.CompatibilityOhaeng;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/** 궁합 오행 시각화 한 행(정규화된 정수 비율)을 저장하는 임베더블. 독립 식별자 없이 SajuCompatibilityJpaEntity에 종속된다. */
@Embeddable
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CompatibilityOhaengEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompatibilityElement element;

    @Column(nullable = false)
    private int percentage;

    public static CompatibilityOhaengEmbeddable fromDomain(CompatibilityOhaeng ohaeng) {
        return CompatibilityOhaengEmbeddable.builder()
                .element(ohaeng.getElement())
                .percentage(ohaeng.getPercentage())
                .build();
    }

    public CompatibilityOhaeng toDomain() {
        return new CompatibilityOhaeng(element, percentage);
    }
}
