package com.yapp.todakun.member.adapter.persistence;

import com.yapp.todakun.member.BirthTime;
import com.yapp.todakun.member.CalendarType;
import com.yapp.todakun.member.Gender;
import com.yapp.todakun.member.Job;
import com.yapp.todakun.member.Member;
import com.yapp.todakun.member.RelationshipStatus;
import com.yapp.todakun.member.Role;
import com.yapp.todakun.persistence.BaseTimeEntity;
import com.yapp.todakun.shared.OAuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "member")
public class MemberJpaEntity extends BaseTimeEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

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
    private OAuthProvider oauthProvider;

    @Column(nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipStatus relationshipStatus;

    protected MemberJpaEntity() {
    }

    private MemberJpaEntity(
            UUID id,
            String name,
            LocalDate birthDate,
            BirthTime birthTime,
            CalendarType calendarType,
            Gender gender,
            Role role,
            OAuthProvider oauthProvider,
            String providerId,
            Job job,
            RelationshipStatus relationshipStatus
    ) {
        this.id = id;
        this.name = name;
        this.birthDate = birthDate;
        this.birthTime = birthTime;
        this.calendarType = calendarType;
        this.gender = gender;
        this.role = role;
        this.oauthProvider = oauthProvider;
        this.providerId = providerId;
        this.job = job;
        this.relationshipStatus = relationshipStatus;
    }

    public static MemberJpaEntity fromDomain(Member member) {
        return new MemberJpaEntity(
                member.getId(),
                member.getName(),
                member.getBirthDate(),
                member.getBirthTime(),
                member.getCalendarType(),
                member.getGender(),
                member.getRole(),
                member.getOauthProvider(),
                member.getProviderId(),
                member.getJob(),
                member.getRelationshipStatus()
        );
    }

    public Member toDomain() {
        return new Member(
                id,
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