package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.fixture.AuthFixture
import com.yapp.todakun.shared.CreateMemberPort
import com.yapp.todakun.shared.CreateSajuChartPort
import com.yapp.todakun.shared.event.MemberSignedUpEvent
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID

private val SAJU_CHART_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000007")

class SignupTransactionServiceTest :
    DescribeSpec(
        {
            val createMemberPort = mockk<CreateMemberPort>()
            val createSajuChartPort = mockk<CreateSajuChartPort>()
            val applicationEventPublisher = mockk<ApplicationEventPublisher>()
            val signupTransactionService = SignupTransactionService(createMemberPort, createSajuChartPort, applicationEventPublisher)

            afterTest { clearMocks(createMemberPort, createSajuChartPort, applicationEventPublisher) }

            val memberId = AuthFixture.MEMBER_ID
            val command = AuthFixture.signupCommand()
            val profile = AuthFixture.oauthMemberProfile()

            describe("register") {
                it("회원을 생성하고 본인(SELF) 사주 명식을 같은 트랜잭션에서 만든 뒤 MemberSignedUpEvent를 발행하고 memberId를 반환한다") {
                    every {
                        createMemberPort.create(
                            provider = profile.provider,
                            providerId = profile.providerId,
                            name = command.name,
                            birthDate = command.birthDate,
                            birthTime = command.birthTime,
                            calendarType = command.calendarType,
                            gender = command.gender,
                            job = command.job,
                            relationshipStatus = command.relationshipStatus,
                        )
                    } returns memberId
                    every {
                        createSajuChartPort.create(
                            memberId = memberId,
                            isSelf = true,
                            name = command.name,
                            gender = command.gender,
                            calendarType = command.calendarType,
                            birthDate = command.birthDate,
                            birthTime = command.birthTime,
                            isLeapMonth = false,
                        )
                    } returns SAJU_CHART_ID
                    every { applicationEventPublisher.publishEvent(any<MemberSignedUpEvent>()) } just Runs

                    signupTransactionService.register(profile, command) shouldBe memberId

                    verify(exactly = 1) {
                        createSajuChartPort.create(
                            memberId = memberId,
                            isSelf = true,
                            name = command.name,
                            gender = command.gender,
                            calendarType = command.calendarType,
                            birthDate = command.birthDate,
                            birthTime = command.birthTime,
                            isLeapMonth = false,
                        )
                    }
                    verify(exactly = 1) { applicationEventPublisher.publishEvent(MemberSignedUpEvent(memberId)) }
                }
            }
        },
    )
