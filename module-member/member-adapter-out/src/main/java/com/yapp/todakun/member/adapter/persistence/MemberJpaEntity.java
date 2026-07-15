package com.yapp.todakun.member.adapter.persistence;

import com.yapp.todakun.member.BirthTime;
import com.yapp.todakun.member.CalendarType;
import com.yapp.todakun.member.Gender;
import com.yapp.todakun.member.Job;
import com.yapp.todakun.member.Member;
import com.yapp.todakun.member.RelationshipStatus;
import com.yapp.todakun.member.Role;
import com.yapp.todakun.persistence.BaseEntity;
import com.yapp.todakun.shared.OauthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
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
                relationshipStatus
        );
    }
}
