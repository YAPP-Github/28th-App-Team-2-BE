package com.yapp.todakun.dailyfortune.adapter.persistence

import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.DailyFortuneNotificationPort
import com.yapp.todakun.shared.NotificationPayload
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

/**
 * [DailyFortuneNotificationPort] 구현체. 당일 운세가 아직 생성되지 않은 회원은 null을 반환해 발송을 스킵시킨다.
 * content는 800자까지 허용되는 운세 본문 전체를 그대로 담기엔 길어, score 기반 한 줄 요약으로 대체한다.
 */
@Component
class DailyFortuneNotificationAdapter(
    private val dailyFortuneRepository: DailyFortuneRepository,
) : DailyFortuneNotificationPort {
    override fun getMorningReport(memberId: UUID): NotificationPayload? {
        val dailyFortune =
            dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, LocalDate.now(SEOUL_ZONE)) ?: return null

        return NotificationPayload(
            title = dailyFortune.title,
            content = "오늘의 운세 점수는 ${dailyFortune.score}점이에요. 지금 확인해 보세요.",
            deepLink = "todakun://fortune/today",
        )
    }
}
