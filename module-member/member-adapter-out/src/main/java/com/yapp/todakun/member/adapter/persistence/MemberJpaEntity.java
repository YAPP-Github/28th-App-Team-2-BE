package com.yapp.todakun.member.adapter.persistence;

import com.yapp.todakun.member.BirthTime;
import com.yapp.todakun.member.CalendarType;
import com.yapp.todakun.member.Gender;
import com.yapp.todakun.member.Job;
import com.yapp.todakun.member.Member;
import com.yapp.todakun.member.RelationshipStatus;
import com.yapp.todakun.member.Role;
import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.shared.FortuneCategory;
import com.yapp.todakun.shared.OauthProvider;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_oauth_provider_id",
                columnNames = {"oauth_provider", "provider_id"}
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BirthTime birthTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalendarType calendarType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OauthProvider oauthProvider;

    @Column(nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipStatus relationshipStatus;

    @ElementCollection
    @CollectionTable(
            name = "member_favorite_fortune_category",
            joinColumns = @JoinColumn(
                    name = "member_id",
                    nullable = false,
                    foreignKey = @ForeignKey(name = "fk_member_favorite_fortune_category_member_id")
            ),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_member_favorite_fortune_category_member_id_fortune_category",
                    columnNames = {"member_id", "fortune_category"}
            )
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "fortune_category", nullable = false)
    private Set<FortuneCategory> favoriteFortuneCategories;

    public static MemberJpaEntity fromDomain(Member member) {
        return MemberJpaEntity.builder()
                .id(member.getId())
                .name(member.getName())
                .birthDate(member.getBirthDate())
                .birthTime(member.getBirthTime())
                .calendarType(member.getCalendarType())
                .gender(member.getGender())
                .role(member.getRole())
                .oauthProvider(member.getOauthProvider())
                .providerId(member.getProviderId())
                .job(member.getJob())
                .relationshipStatus(member.getRelationshipStatus())
                .favoriteFortuneCategories(member.getFavoriteFortuneCategories())
                .build();
    }

    public Member toDomain() {
        return Member.reconstitute(
                getId(),
                name,
                birthDate,
                birthTime,
                calendarType,
                gender,
                role,
                oauthProvider,
                providerId,
                job,
                relationshipStatus,
                // @ElementCollection(LAZY)인 PersistentSet을 도메인에 그대로 넘기면 세션 종료(OSIV off) 후
                // 접근 시 LazyInitializationException이 난다. 트랜잭션 안에서 방어적 복사로 초기화 + 프록시 분리.
                new HashSet<>(favoriteFortuneCategories)
        );
    }
}
