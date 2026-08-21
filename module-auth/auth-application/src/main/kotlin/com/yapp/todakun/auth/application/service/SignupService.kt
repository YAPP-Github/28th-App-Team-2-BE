package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.exception.OnboardingTokenInvalidException
import com.yapp.todakun.auth.port.inbound.SignupCommand
import com.yapp.todakun.auth.port.inbound.SignupResult
import com.yapp.todakun.auth.port.inbound.SignupUseCase
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.GetMemberIdPort
import com.yapp.todakun.shared.currentDate
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 회원가입 유스케이스(오케스트레이터). 의도적으로 트랜잭션을 걸지 않는다 —
 * 당일 운세 생성이 수십 초 걸리는 외부 AI 호출이라, 그 구간 동안 DB 커넥션을 점유하면 안 되기 때문이다.
 * 회원·사주 명식 저장만 [SignupTransactionService]의 짧은 트랜잭션으로 위임한다.
 *
 * 이 순서 덕분에 계정은 AI 호출이 시작되기 전에 이미 확정된다. 응답이 늦어 클라이언트가 타임아웃해도
 * 재로그인이 곧바로 기존 회원 로그인으로 처리되고, 중복으로 들어온 회원가입 요청도
 * "이미 가입된 회원"(409)이 아니라 멱등하게 성공한다.
 */
@Service
@Loggable
class SignupService(
    private val onboardingTokenPort: OnboardingTokenPort,
    private val signupTransactionService: SignupTransactionService,
    private val getMemberIdPort: GetMemberIdPort,
    private val createDailyFortunePort: CreateDailyFortunePort,
    private val accessTokenPort: AccessTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
) : SignupUseCase {
    override fun signup(command: SignupCommand): SignupResult {
        val profile =
            onboardingTokenPort.findProfile(command.onboardingToken)
                ?: throw OnboardingTokenInvalidException()

        // 멱등 처리: 이미 같은 소셜 계정으로 가입돼 있으면 다시 만들지 않고 토큰만 발급한다.
        // 첫 요청이 타임아웃돼 클라이언트가 재시도한 경우가 여기 해당하며, 재가입이 아니라 로그인과 같은 결과를 준다.
        getMemberIdPort.findIdByOauth(profile.provider, profile.providerId)?.let { return issueTokens(it, command) }

        val memberId =
            try {
                signupTransactionService.register(profile, command)
            } catch (e: BusinessException) {
                // 동시에 들어온 중복 회원가입 요청과 경합해 회원 유니크 제약에 걸린 경우다.
                // 상대 요청이 먼저 커밋했다면 그 회원으로 멱등 처리하고(당일 운세도 상대 요청이 만든다),
                // 아니라면 원래 예외를 그대로 던진다.
                val winner = getMemberIdPort.findIdByOauth(profile.provider, profile.providerId) ?: throw e

                return issueTokens(winner, command)
            }

        generateFirstDailyFortune(memberId)

        return issueTokens(memberId, command)
    }

    private fun issueTokens(
        memberId: UUID,
        command: SignupCommand,
    ): SignupResult {
        val result =
            SignupResult(
                // 회원가입으로 만들어지는 회원은 항상 Role.MEMBER라 조회 없이 isAdmin=false로 발급한다.
                // 멱등 경로로 들어온 기존 회원도 직전 회원가입이 만든 회원이므로 동일하다.
                accessToken = accessTokenPort.generate(memberId, isAdmin = false),
                refreshToken = refreshTokenPort.issue(memberId),
            )

        // 성공했을 때만 폐기한다 — 실패했는데 폐기하면 클라이언트가 같은 온보딩 토큰으로 재시도할 수 없다.
        onboardingTokenPort.revoke(command.onboardingToken)

        return result
    }

    /**
     * 가입 직후 당일 운세를 생성한다(트랜잭션 밖 — 외부 AI 호출).
     * 신규 가입자는 이전 서비스 데이의 연속성을 지킬 필요가 없으므로 롤오버 규칙 대신 실제 캘린더 날짜를 쓴다.
     * 실패해도 이미 확정된 가입을 되돌리지 않는다 — 최초 조회(GetTodayFortuneService)나 다음 배치가 채운다.
     */
    private fun generateFirstDailyFortune(memberId: UUID) {
        try {
            createDailyFortunePort.create(memberId, currentDate())
        } catch (e: Exception) {
            log.warn("가입 직후 오늘의 운세 생성 실패(가입은 유지, 최초 조회 시 재생성): memberId={}", memberId, e)
        }
    }
}
