package com.yapp.todakun.terms

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 약관. 서비스가 제공하는 개별 약관 항목이며, 회원은 이 약관에 대해 동의/미동의 의사를 남긴다.
 * [required]가 true인 약관은 회원가입/온보딩 시 반드시 동의해야 한다.
 */
data class Terms(
    val id: UUID,
    val type: TermsType,
    val title: String,
    val required: Boolean,
) {
    companion object {
        @ExperimentalUuidApi
        fun create(
            type: TermsType,
            title: String,
            required: Boolean,
        ): Terms =
            Terms(
                id = Uuid.generateV7().toJavaUuid(),
                type = type,
                title = title,
                required = required,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            type: TermsType,
            title: String,
            required: Boolean,
        ): Terms =
            Terms(
                id = id,
                type = type,
                title = title,
                required = required,
            )
    }
}
