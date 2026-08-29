package com.yapp.todakun.dailyfortune.port.outbound

import java.time.LocalDate

/**
 * 오늘의 운세를 AI로 생성하는 아웃바운드 포트다.
 * 프롬프트 구성·모델 호출·구조화 매핑은 어댑터가 담당한다.
 */
interface DailyFortuneAiPort {
    fun generate(
        profile: MemberSajuProfile,
        fortuneDate: LocalDate,
        todayPillar: Pillar,
    ): GeneratedDailyFortune
}
