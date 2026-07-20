package com.yapp.todakun.saju.port.outbound

import com.yapp.todakun.saju.SajuChart
import java.util.UUID

/** 사주 명식 애그리거트 영속화 아웃바운드 포트. 애그리거트(헤더+4주+오행+십성 집계)를 통째로 저장/조회/삭제한다. */
interface SajuChartRepository {
    fun save(sajuChart: SajuChart): SajuChart

    fun findById(id: UUID): SajuChart?

    /** 명식과 그 자식(4주·오행·십성)을 함께 삭제한다. */
    fun deleteById(id: UUID)

    /** 여러 명식과 자식들을 일괄 삭제한다(탈퇴 시 회원의 전체 명식 제거). */
    fun deleteAllByIds(ids: Collection<UUID>)
}
