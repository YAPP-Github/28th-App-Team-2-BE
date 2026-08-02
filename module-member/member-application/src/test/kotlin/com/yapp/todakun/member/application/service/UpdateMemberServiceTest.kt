package com.yapp.todakun.member.application.service

import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.RelationshipStatus
import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.fixture.MemberFixture
import com.yapp.todakun.member.port.inbound.UpdateMemberCommand
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.ReplaceSelfSajuChartPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class UpdateMemberServiceTest : DescribeSpec({
    val memberRepository = mockk<MemberRepository>()
    val replaceSelfSajuChartPort = mockk<ReplaceSelfSajuChartPort>()
    val service = UpdateMemberService(memberRepository, replaceSelfSajuChartPort)

    afterTest { clearMocks(memberRepository, replaceSelfSajuChartPort) }

    // 기존 회원: 생년 1999-01-01, 시간 모름, 양력, 여성
    fun command(
        gender: Gender = Gender.FEMALE,
        calendarType: CalendarType = CalendarType.SOLAR,
        birthDate: LocalDate = LocalDate.of(1999, 1, 1),
        birthTime: BirthTime = BirthTime.UNKNOWN,
    ) = UpdateMemberCommand(
        memberId = MemberFixture.MEMBER_ID,
        gender = gender,
        calendarType = calendarType,
        birthDate = birthDate,
        birthTime = birthTime,
        job = Job.WORKER,
        relationshipStatus = RelationshipStatus.DATING,
    )

    describe("update") {
        context("생년 정보가 그대로면(관심 주제·현재 상황만 변경)") {
            it("회원만 저장하고 사주 재계산은 하지 않는다") {
                every { memberRepository.findById(MemberFixture.MEMBER_ID) } returns MemberFixture.member()
                every { memberRepository.save(any()) } answers { firstArg() }

                service.update(command())

                verify(exactly = 1) { memberRepository.save(any()) }
                verify(exactly = 0) { replaceSelfSajuChartPort.replace(any(), any(), any(), any(), any(), any(), any()) }
            }
        }

        context("생년 정보가 바뀌면") {
            it("회원을 저장하고 SELF 사주 명식을 재계산한다") {
                every { memberRepository.findById(MemberFixture.MEMBER_ID) } returns MemberFixture.member()
                every { memberRepository.save(any()) } answers { firstArg() }
                every { replaceSelfSajuChartPort.replace(any(), any(), any(), any(), any(), any(), any()) } just Runs

                service.update(command(birthDate = LocalDate.of(2001, 5, 30), birthTime = BirthTime.MISI))

                verify(exactly = 1) { memberRepository.save(any()) }
                verify(exactly = 1) {
                    replaceSelfSajuChartPort.replace(
                        memberId = MemberFixture.MEMBER_ID,
                        name = "홍길동",
                        gender = "FEMALE",
                        calendarType = "SOLAR",
                        birthDate = LocalDate.of(2001, 5, 30),
                        birthTime = "MISI",
                        isLeapMonth = false,
                    )
                }
            }
        }

        context("회원이 존재하지 않으면") {
            it("MemberNotFoundException을 던진다") {
                every { memberRepository.findById(MemberFixture.MEMBER_ID) } returns null

                shouldThrow<MemberNotFoundException> { service.update(command()) }

                verify(exactly = 0) { memberRepository.save(any()) }
            }
        }
    }
})
