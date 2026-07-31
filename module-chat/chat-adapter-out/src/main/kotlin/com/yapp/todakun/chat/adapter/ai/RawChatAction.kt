package com.yapp.todakun.chat.adapter.ai

import com.yapp.todakun.chat.ChatAction
import com.yapp.todakun.chat.ChatActionType
import java.time.LocalDate

/**
 * 액션 카드 구조화 출력의 원본 파싱 형태. [date]를 문자열(ISO-8601, "yyyy-MM-dd")로 받아 어댑터에서 직접 파싱한다 —
 * LocalDate로 바로 매핑을 시도하면 모델이 형식에서 벗어난 값을 낼 때 원인을 알기 어려운 역직렬화 실패로 이어지기 때문이다.
 * [hasAction]이 false거나 나머지 필드 중 하나라도 비어 있으면 액션 카드가 없다는 뜻이다.
 */
data class RawChatAction(
    val hasAction: Boolean,
    val type: ChatActionType?,
    val label: String?,
    val category: String?,
    val date: String?,
) {
    fun toDomainOrNull(): ChatAction? {
        if (!hasAction || type == null || label == null || category == null || date == null) return null
        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null

        return ChatAction(type = type, label = label, category = category, date = parsedDate)
    }
}
