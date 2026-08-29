package com.yapp.todakun.dayfortune.port.outbound

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import java.time.LocalDate

/**
 * 택일 운세를 AI로 생성하는 아웃바운드 포트다.
 * 프롬프트 구성·모델 호출·구조화 매핑은 어댑터가 담당한다.
 */
interface DaySelectionFortuneAiPort {
    fun generate(
        profile: MemberSajuProfile,
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
        dayPillar: Pillar,
    ): GeneratedDaySelectionFortune
}
