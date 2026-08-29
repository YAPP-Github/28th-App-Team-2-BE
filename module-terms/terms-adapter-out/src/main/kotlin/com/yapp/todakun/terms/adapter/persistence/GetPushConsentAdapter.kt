package com.yapp.todakun.terms.adapter.persistence

import com.yapp.todakun.shared.GetPushConsentPort
import com.yapp.todakun.terms.TermsType
import com.yapp.todakun.terms.repository.MemberTermsAgreementRepository
import com.yapp.todakun.terms.repository.TermsRepository
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 알림 발송 대상 회원의 야간 푸시 수신 동의 조회(shared.GetPushConsentPort 구현).
 * TermsType.NIGHT_PUSH 약관에 동의한 회원만 야간 푸시 대상으로 판단한다.
 * MSA 분리 시 이 구현체만 HTTP 어댑터로 교체하면 notification 측 코드는 바뀌지 않는다.
 */
@Component
class GetPushConsentAdapter(
    private val termsRepository: TermsRepository,
    private val memberTermsAgreementRepository: MemberTermsAgreementRepository,
) : GetPushConsentPort {
    override fun isNightPushAllowed(memberId: UUID): Boolean {
        val nightPushTermsIds =
            termsRepository.findAllByType(TermsType.NIGHT_PUSH).map { it.id }.toSet()
        // 야간 푸시 약관이 정의돼 있지 않으면 별도 제한을 두지 않는다.
        if (nightPushTermsIds.isEmpty()) return true
        return memberTermsAgreementRepository
            .findAllByMemberId(memberId)
            .any { it.termsId in nightPushTermsIds && it.agreed }
    }
}
