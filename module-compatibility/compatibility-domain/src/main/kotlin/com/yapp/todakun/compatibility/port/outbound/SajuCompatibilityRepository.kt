package com.yapp.todakun.compatibility.port.outbound

import com.yapp.todakun.compatibility.SajuCompatibility
import java.util.UUID

/** 궁합 결과 애그리거트(헤더 + 오행 비율) 영속화 아웃바운드 포트. */
interface SajuCompatibilityRepository {
    fun save(compatibility: SajuCompatibility): SajuCompatibility

    fun findByMemberIdAndCharts(
        memberId: UUID,
        myChartId: UUID,
        partnerChartId: UUID,
    ): SajuCompatibility?

    /** (myChartId, partnerChartId) 기준 트랜잭션 스코프 DB 락(커밋·롤백 시 자동 해제). 동시 생성 요청을 직렬화해 유니크 제약 충돌을 막는다. */
    fun lock(
        myChartId: UUID,
        partnerChartId: UUID,
    )
}
