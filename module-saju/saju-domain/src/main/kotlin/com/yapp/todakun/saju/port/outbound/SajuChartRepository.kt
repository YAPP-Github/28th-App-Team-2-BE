package com.yapp.todakun.saju.port.outbound

import com.yapp.todakun.saju.SajuChart
import java.util.UUID

/** 사주 명식 영속화 아웃바운드 포트. 애그리거트(헤더+4주+오행+십성 집계)를 통째로 저장/조회한다. */
interface SajuChartRepository {
    fun save(sajuChart: SajuChart): SajuChart

    fun findById(id: UUID): SajuChart?
}
