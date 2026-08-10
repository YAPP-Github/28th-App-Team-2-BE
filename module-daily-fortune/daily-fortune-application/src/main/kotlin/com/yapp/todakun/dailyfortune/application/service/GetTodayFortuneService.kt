package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.dailyfortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.dailyfortune.port.inbound.GetTodayFortuneUseCase
import com.yapp.todakun.dailyfortune.port.inbound.TodayFortuneSummary
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.GetLuckActionScoresPort
import com.yapp.todakun.shared.currentDate
import org.springframework.cache.annotation.Cacheable
import java.time.LocalDate
import java.util.UUID

@QueryService
class GetTodayFortuneService(
    private val dailyFortuneRepository: DailyFortuneRepository,
    private val getLuckActionScoresPort: GetLuckActionScoresPort,
) : GetTodayFortuneUseCase {
    // 회원별 하루 1건, 생성 후 불변이라 evict 없이 TTL(CacheNames 참고)만으로 무효화한다(이슈 #56).
    @Cacheable(cacheNames = [CacheNames.TODAY_FORTUNE], key = "#memberId + ':' + #fortuneDate")
    override fun getToday(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): TodayFortuneSummary {
        // 온보딩 시 생성되는 신규 가입자의 첫 오늘의 운세는 서비스 데이 롤오버 없이 현재 날짜(currentDate())로 저장된다.
        // 그래서 자정~06:00 사이 가입 직후 조회하면 서비스 데이(fortuneDate, 전날)로는 못 찾을 수 있어 현재 날짜로 한 번 더 조회한다.
        val dailyFortune =
            dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, fortuneDate)
                ?: dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, currentDate())
                ?: throw DailyFortuneNotFoundException()

        val luckActionScores = getLuckActionScoresPort.getScores(memberId, dailyFortune.fortuneDate)

        return TodayFortuneSummary.from(dailyFortune, luckActionScores)
    }
}
