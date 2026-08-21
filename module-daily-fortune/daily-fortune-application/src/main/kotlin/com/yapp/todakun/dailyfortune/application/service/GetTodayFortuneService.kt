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
        try {
            createDailyFortunePort.create(memberId, currentDate())
        } catch (e: Exception) {
            // 내 생성이 실패해도 경합 상대나 배치가 이미 저장해 뒀을 수 있어 한 번 더 조회한다.
            // 그래도 없으면 AI 실패 원인(503 회로 차단·504 지연 등)을 404로 가리지 않고 그대로 전달한다 —
            // "잠시 후 다시 시도" 같은 재시도 안내가 "운세 없음"보다 클라이언트에 유용하기 때문이다.
            log.warn("조회 시점 오늘의 운세 생성 실패: memberId={}, fortuneDate={}", memberId, fortuneDate, e)

            return todayFortuneReader.find(memberId, fortuneDate) ?: throw e
        }

        return todayFortuneReader.find(memberId, fortuneDate) ?: throw DailyFortuneNotFoundException()
    }
}
