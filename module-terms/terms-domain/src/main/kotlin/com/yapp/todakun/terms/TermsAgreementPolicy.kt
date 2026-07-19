package com.yapp.todakun.terms

import com.yapp.todakun.terms.exception.RequiredTermsNotAgreedException
import com.yapp.todakun.terms.exception.TermsNotFoundException
import java.util.UUID

/**
 * 약관 동의 제출의 도메인 불변식을 검증한다.
 * - 제출된 약관 ID는 모두 실제 약관 카탈로그에 존재해야 한다.
 * - 필수 약관은 빠짐없이 동의(agreed = true)되어야 한다.
 */
object TermsAgreementPolicy {
    fun validate(
        catalog: List<Terms>,
        submittedTermsIds: Collection<UUID>,
        agreedTermsIds: Set<UUID>,
    ) {
        val catalogIds = catalog.map { it.id }.toSet()
        if (submittedTermsIds.any { it !in catalogIds }) {
            throw TermsNotFoundException()
        }

        if (catalog.any { it.required && it.id !in agreedTermsIds }) {
            throw RequiredTermsNotAgreedException()
        }
    }
}
