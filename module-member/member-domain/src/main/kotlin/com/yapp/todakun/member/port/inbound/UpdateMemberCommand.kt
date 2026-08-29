package com.yapp.todakun.member.port.inbound

import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.RelationshipStatus
import java.time.LocalDate
import java.util.UUID

/** 내 정보 수정 커맨드. 생년 정보가 바뀌면 SELF 사주 명식을 재계산한다. */
data class UpdateMemberCommand(
    val memberId: UUID,
    val gender: Gender,
    val calendarType: CalendarType,
    val birthDate: LocalDate,
    val birthTime: BirthTime,
    val job: Job,
    val relationshipStatus: RelationshipStatus,
)
