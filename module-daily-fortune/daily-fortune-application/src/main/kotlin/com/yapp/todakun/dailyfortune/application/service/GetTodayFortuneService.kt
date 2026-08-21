package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.dailyfortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.dailyfortune.port.inbound.GetTodayFortuneUseCase
import com.yapp.todakun.dailyfortune.port.inbound.TodayFortuneSummary
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.currentDate
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

/**
 * 오늘의 운세 요약 조회. 의도적으로 트랜잭션을 걸지 않는다 —
 * 아직 생성되지 않았을 때 [CreateDailyFortunePort]의 AI 호출을 트랜잭션 밖에서 수행하기 위함이다.
 * 조회 자체는 [TodayFortuneReader](읽기 트랜잭션)에 위임한다.
 */
@Service
@Loggable
class GetTodayFortuneService(
    private val todayFortuneReader: TodayFortuneReader,
    private val createDailyFortunePort: CreateDailyFortunePort,
) : GetTodayFortuneUseCase {
    // 회원별 하루 1건, 생성 후 불변이라 evict 없이 TTL(CacheNames 참고)만으로 무효화한다(이슈 #56).
    @Cacheable(cacheNames = [CacheNames.TODAY_FORTUNE], key = "#memberId + ':' + #fortuneDate")
    override fun getToday(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): TodayFortuneSummary {
        todayFortuneReader.find(memberId, fortuneDate)?.let { return it }

        // 가입 직후 AI 실패나 배치 skip으로 운세가 비어 있는 회원을 조회 시점에 자가 치유한다.
        // 생성 자체가 멱등(락 + saveIfAbsent)이라 동시 요청이 들어와도 한 건만 저장된다.
        log.info("오늘의 운세가 없어 조회 시점에 생성한다: memberId={}, fortuneDate={}", memberId, fortuneDate)
        createDailyFortunePort.create(memberId, currentDate())

        return todayFortuneReader.find(memberId, fortuneDate) ?: throw DailyFortuneNotFoundException()
    }
}
