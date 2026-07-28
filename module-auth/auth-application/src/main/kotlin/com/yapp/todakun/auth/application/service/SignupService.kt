package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.exception.OnboardingTokenInvalidException
import com.yapp.todakun.auth.port.inbound.SignupCommand
import com.yapp.todakun.auth.port.inbound.SignupResult
import com.yapp.todakun.auth.port.inbound.SignupUseCase
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.CreateMemberPort
import com.yapp.todakun.shared.CreateSajuChartPort
import com.yapp.todakun.shared.currentDate

@CommandService
class SignupService(
    private val onboardingTokenPort: OnboardingTokenPort,
    private val createMemberPort: CreateMemberPort,
    private val createSajuChartPort: CreateSajuChartPort,
    private val createDailyFortunePort: CreateDailyFortunePort,
    private val accessTokenPort: AccessTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
) : SignupUseCase {
    override fun signup(command: SignupCommand): SignupResult {
        val profile =
            onboardingTokenPort.findProfile(command.onboardingToken)
                ?: throw OnboardingTokenInvalidException()

        try {
            val memberId =
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
                    favoriteFortuneCategories = command.favoriteFortuneCategories,
                )

            // 회원 본인 사주 명식을 같은 트랜잭션에서 계산·저장(원자성 보장).
            // 음력 입력은 평달로 간주해 isLeapMonth=false로 고정한다(윤달 미지원 — 대부분의 만세력 서비스와 동일 정책).
            // 알려진 한계: 음력 윤달 생일자는 평달로 계산됨. 윤달 지원이 필요해지면 SignupCommand에 윤달 필드를 추가한다.
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

            // 신규 회원의 당일 운세도 같은 트랜잭션에서 생성해 원자성을 보장한다(실패 시 회원가입 전체 롤백).
            // 신규 가입자는 이전 서비스 데이의 연속성을 지킬 필요가 없으므로 롤오버 규칙 대신 실제 캘린더 날짜를 쓴다.
            createDailyFortunePort.create(
                memberId = memberId,
                fortuneDate = currentDate(),
            )

            return SignupResult(
                accessToken = accessTokenPort.generate(memberId),
                refreshToken = refreshTokenPort.issue(memberId),
            )
        } finally {
            onboardingTokenPort.revoke(command.onboardingToken)
        }
    }
}
