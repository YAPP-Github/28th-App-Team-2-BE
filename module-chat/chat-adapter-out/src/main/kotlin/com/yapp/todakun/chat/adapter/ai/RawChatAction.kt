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
    /**
     * @param today KST 기준 오늘. 프롬프트로 기준일을 못 박아도 모델이 학습 데이터에 이끌려 과거 날짜를 낼 수 있으므로,
     *   캘린더에 담을 수 없는 지난 날짜는 여기서 한 번 더 걸러 액션 카드 없음으로 처리한다.
     */
    fun toDomainOrNull(today: LocalDate): ChatAction? {
        if (!hasAction) return null
        val validType = type ?: return null
        val validLabel = label?.takeIf { it.isNotBlank() } ?: return null
        val validCategory = category?.takeIf { it.isNotBlank() } ?: return null
        val validDate = date ?: return null
        val parsedDate = runCatching { LocalDate.parse(validDate) }.getOrNull() ?: return null
        if (parsedDate.isBefore(today)) return null

        return ChatAction(type = validType, label = validLabel, category = validCategory, date = parsedDate)
    }
}
