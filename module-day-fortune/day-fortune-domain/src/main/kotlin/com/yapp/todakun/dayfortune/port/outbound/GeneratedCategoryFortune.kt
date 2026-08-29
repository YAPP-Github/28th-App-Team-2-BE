package com.yapp.todakun.dayfortune.port.outbound

import com.yapp.todakun.shared.FortuneCategory

/**
 * AI가 생성한 카테고리별 운세 별점 한 건을 담는다.
 * 5개 카테고리 중 별점 상위 3개(동점 시 유사도 우선, 그래도 동률이면 무작위)를 AI가 프롬프트 단에서 이미 선정해 반환하므로
 * 여기에는 최종 선정된 카테고리만 담기며, 앱 코드는 별도의 동점 처리 로직을 갖지 않는다.
 */
data class GeneratedCategoryFortune(
    val fortuneCategory: FortuneCategory,
    val star: Int,
)
