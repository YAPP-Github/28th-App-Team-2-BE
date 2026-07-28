package com.yapp.todakun.dailyfortune.adapter.scheduler

import com.yapp.todakun.dailyfortune.port.inbound.GenerateDailyFortunesUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

private const val SEOUL = "Asia/Seoul"

/**
 * 일일 운세 생성의 시각 트리거(driving adapter).
 * 회원 선별·생성은 application 유스케이스가 담당한다.
 * 스케줄은 Asia/Seoul(KST) 기준이다.
 */
@Component
class DailyFortuneScheduler(
    private val generateDailyFortunesUseCase: GenerateDailyFortunesUseCase,
) {
    // 매일 03:00(KST), 전체 회원의 오늘의 운세를 미리 생성한다.
    @Scheduled(cron = "0 0 3 * * *", zone = SEOUL)
    fun generateTodayFortunes() = generateDailyFortunesUseCase.generate(LocalDate.now(ZoneId.of(SEOUL)))
}
