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
import com.yapp.todakun.shared.GetMemberIdPort
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 회원가입 유스케이스(오케스트레이터). 의도적으로 트랜잭션을 걸지 않는다.
 * 회원·사주 명식 저장만 [SignupTransactionService]의 짧은 트랜잭션으로 위임하고,
 * 그 결과(성공/멱등 충돌)에 따라 토큰만 발급한다. 당일 운세 생성(외부 AI 호출)은 이 클래스가 직접 하지 않는다.
 * [SignupTransactionService]가 트랜잭션 커밋 후 발행하는 이벤트를 받아 전용 리스너가 비동기로 처리하므로, 이 메서드는 AI 응답을 기다리지 않는다.
 *
 * 계정은 회원가입 응답이 나가기 전에 이미 확정된다. 응답이 늦어 클라이언트가 타임아웃해도
 * 재로그인이 곧바로 기존 회원 로그인으로 처리되고, 중복으로 들어온 회원가입 요청도
 * "이미 가입된 회원"(409)이 아니라 멱등하게 성공한다.
 */
@Service
@Loggable
class SignupService(
    private val onboardingTokenPort: OnboardingTokenPort,
    private val signupTransactionService: SignupTransactionService,
    private val getMemberIdPort: GetMemberIdPort,
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

                // 삼킨 예외는 응답에 드러나지 않으므로, 회원 유니크 제약이 아닌 다른 도메인 예외가 섞여 들어와도
                // 운영에서 추적할 수 있도록 코드를 남긴다.
                log.info("회원 생성 경합으로 기존 회원에 멱등 처리: memberId={}, errorCode={}", winner, e.errorCode.code)

                return issueTokens(winner, command)
            }

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
}
