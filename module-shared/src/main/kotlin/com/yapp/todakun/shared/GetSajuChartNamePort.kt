package com.yapp.todakun.shared

import java.util.UUID

/**
 * 궁합 조회(compatibility 도메인)가 저장된 궁합의 상대 명식 이름을 조회하는 확장점.
 * 궁합 레코드에는 이름을 저장하지 않고 명식이 이름의 단일 원천이므로, 조회 시점에 이 포트로 가져온다.
 */
interface GetSajuChartNamePort {
    fun getName(chartId: UUID): String?
}
