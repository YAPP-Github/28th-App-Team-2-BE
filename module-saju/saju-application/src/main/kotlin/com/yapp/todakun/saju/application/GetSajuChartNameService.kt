package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.GetSajuChartNamePort
import java.util.UUID

/** 명식 ID로 이름을 조회하는 크로스 도메인 유스케이스([GetSajuChartNamePort] 구현). */
@QueryService
class GetSajuChartNameService(
    private val sajuChartRepository: SajuChartRepository,
) : GetSajuChartNamePort {
    override fun getName(chartId: UUID): String? = sajuChartRepository.findById(chartId)?.name
}
