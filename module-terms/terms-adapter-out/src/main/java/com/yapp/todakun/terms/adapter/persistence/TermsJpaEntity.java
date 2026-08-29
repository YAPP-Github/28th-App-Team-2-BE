package com.yapp.todakun.terms.adapter.persistence;

import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.terms.Terms;
import com.yapp.todakun.terms.TermsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "terms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_terms_type",
                columnNames = {"type"}
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TermsJpaEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TermsType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean required;

    public static TermsJpaEntity fromDomain(Terms terms) {
        return TermsJpaEntity.builder()
                .id(terms.getId())
                .type(terms.getType())
                .title(terms.getTitle())
                .required(terms.getRequired())
                .build();
    }

    public Terms toDomain() {
        return Terms.reconstitute(
                getId(),
                type,
                title,
                required
        );
    }
}
