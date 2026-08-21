package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.dailyfortune.port.inbound.TodayFortuneSummary
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.GetLuckActionScoresPort
import com.yapp.todakun.shared.currentDate
import java.time.LocalDate
import java.util.UUID

/**
 * 오늘의 운세 요약 조회의 읽기 트랜잭션 경계를 소유하는 협력 빈.
 * 아직 생성되지 않은 경우 [GetTodayFortuneService]가 트랜잭션 밖에서 AI 생성을 수행해야 해서, 조회만 분리했다.
 */
@QueryService
class TodayFortuneReader(
    private val dailyFortuneRepository: DailyFortuneRepository,
    private val getLuckActionScoresPort: GetLuckActionScoresPort,
) {
    /** 오늘의 운세 요약을 조회한다. 아직 생성되지 않았으면 null을 반환한다. */
    fun find(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): TodayFortuneSummary? {
        // 온보딩 시 생성되는 신규 가입자의 첫 오늘의 운세는 서비스 데이 롤오버 없이 현재 날짜(currentDate())로 저장된다.
        // 그래서 자정~06:00 사이 가입 직후 조회하면 서비스 데이(fortuneDate, 전날)로는 못 찾을 수 있어 현재 날짜로 한 번 더 조회한다.
        val dailyFortune =
            dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, fortuneDate)
                ?: dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, currentDate())
                ?: return null

        val luckActionScores = getLuckActionScoresPort.getScores(memberId, dailyFortune.fortuneDate)

        return TodayFortuneSummary.from(dailyFortune, luckActionScores)
    }
}
