package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.port.inbound.SignupCommand
import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.shared.CreateMemberPort
import com.yapp.todakun.shared.CreateSajuChartPort
import com.yapp.todakun.shared.event.MemberSignedUpEvent
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID

/**
 * 회원가입의 DB 트랜잭션 경계를 소유하는 협력 빈.
 * 회원 생성과 본인(SELF) 사주 명식 생성만 한 트랜잭션으로 묶어, 둘 중 하나만 남는 상태를 막는다.
 * 당일 운세 생성(외부 AI 호출)은 이 트랜잭션의 커밋 이후에만 시작돼야 하므로,
 * 직접 호출하지 않고 [MemberSignedUpEvent]를 발행해 `@TransactionalEventListener(AFTER_COMMIT)` 리스너에 위임한다.
 */
@CommandService
class SignupTransactionService(
    private val createMemberPort: CreateMemberPort,
    private val createSajuChartPort: CreateSajuChartPort,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    fun register(
        profile: OauthMemberProfile,
        command: SignupCommand,
    ): UUID {
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
            )

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

        // AFTER_COMMIT 리스너가 처리하므로 커밋 전 이벤트가 먼저 처리되는 경쟁이 생기지 않는다.
        applicationEventPublisher.publishEvent(MemberSignedUpEvent(memberId))

        return memberId
    }
}
