package com.yapp.todakun.dailyfortune.port.outbound

import com.yapp.todakun.shared.FortuneCategory
import java.time.LocalDate

/**
 * 회원 프로필과 출생 사주 명식(평생 고정값)을 담는다.
 * member/saju 도메인 타입을 직접 참조하지 않도록 원시 타입으로만 구성하며, shared 포트가 이 형태로 변환해 전달한다.
 * 날짜별로 달라지는 값(오늘 일진 등)은 여기 두지 않고 [DailyFortuneAiPort.generate]의 별도 파라미터로 전달한다.
 * 이름은 포함하지 않는다(콘텐츠 생성에 쓰이지 않는 식별자를 외부 AI로 보내지 않기 위한 개인정보 최소화).
 */
data class MemberSajuProfile(
    val birthDate: LocalDate,
    val gender: String,
    val job: String,
    val relationshipStatus: String,
    val favoriteFortuneCategories: List<FortuneCategory>,
    val dayMaster: String,
    val yearPillar: Pillar,
    val monthPillar: Pillar,
    val dayPillar: Pillar,
    val hourPillar: Pillar?,
    val ohaeng: Map<String, Int>,
    val sipseong: Map<String, Int>,
)
