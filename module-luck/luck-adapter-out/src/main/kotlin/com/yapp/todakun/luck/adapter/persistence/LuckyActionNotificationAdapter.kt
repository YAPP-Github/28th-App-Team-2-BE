package com.yapp.todakun.luck.adapter.persistence

import com.yapp.todakun.luck.repository.LuckActionRepository
import com.yapp.todakun.shared.LuckyActionNotificationPort
import com.yapp.todakun.shared.NotificationPayload
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

/**
 * [LuckyActionNotificationPort] 구현체. 오늘 행운 액션이 없거나 전부 완료한 회원은 null을 반환해 발송을 스킵시킨다.
 */
@Component
class LuckyActionNotificationAdapter(
    private val luckActionRepository: LuckActionRepository,
) : LuckyActionNotificationPort {
    override fun getLuckyActionReminder(memberId: UUID): NotificationPayload? {
        val incompleteCount =
            luckActionRepository
                .findAllByMemberIdAndFortuneDate(memberId, LocalDate.now(SEOUL_ZONE))
                .count { !it.achieved }
        if (incompleteCount == 0) return null

        return NotificationPayload(
            title = "오늘의 행운 액션",
            content = "완료하지 않은 행운 액션이 ${incompleteCount}개 있어요. 지금 확인해 보세요.",
            deepLink = "todakun://lucky-action",
        )
    }
}
