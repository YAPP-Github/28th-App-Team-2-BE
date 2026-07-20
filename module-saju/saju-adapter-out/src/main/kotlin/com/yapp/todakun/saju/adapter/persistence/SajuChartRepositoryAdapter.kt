package com.yapp.todakun.saju.adapter.persistence

import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 사주 명식 애그리거트 영속 어댑터. 헤더 + 4주 + 오행/십성 집계를 각 테이블에 나눠 저장하고,
 * 조회 시 chartId로 자식들을 모아 도메인 애그리거트로 복원한다(트랜잭션은 application 서비스가 관리).
 */
@Repository
class SajuChartRepositoryAdapter(
    private val chartJpaRepository: SajuChartJpaRepository,
    private val pillarJpaRepository: SajuPillarJpaRepository,
    private val ohaengJpaRepository: SajuOhaengJpaRepository,
    private val sipseongJpaRepository: SajuSipseongJpaRepository,
) : SajuChartRepository {
    @ExperimentalUuidApi
    override fun save(sajuChart: SajuChart): SajuChart {
        chartJpaRepository.save(SajuChartJpaEntity.fromDomain(sajuChart))
        pillarJpaRepository.saveAll(
            sajuChart.pillars.map { SajuPillarJpaEntity.fromDomain(newId(), sajuChart.id, it) },
        )
        ohaengJpaRepository.saveAll(
            sajuChart.ohaeng.map { SajuOhaengJpaEntity.fromDomain(newId(), sajuChart.id, it) },
        )
        sipseongJpaRepository.saveAll(
            sajuChart.sipseong.map { SajuSipseongJpaEntity.fromDomain(newId(), sajuChart.id, it) },
        )
        return sajuChart
    }

    override fun findById(id: UUID): SajuChart? {
        val chart = chartJpaRepository.findById(id).orElse(null) ?: return null
        return chart.toDomain(
            pillarJpaRepository.findByChartId(id).map { it.toDomain() },
            ohaengJpaRepository.findByChartId(id).map { it.toDomain() },
            sipseongJpaRepository.findByChartId(id).map { it.toDomain() },
        )
    }

    override fun deleteById(id: UUID) = deleteAllByIds(listOf(id))

    override fun deleteAllByIds(ids: Collection<UUID>) {
        if (ids.isEmpty()) return
        pillarJpaRepository.deleteByChartIdIn(ids)
        ohaengJpaRepository.deleteByChartIdIn(ids)
        sipseongJpaRepository.deleteByChartIdIn(ids)
        chartJpaRepository.deleteAllById(ids)
    }

    @ExperimentalUuidApi
    private fun newId(): UUID = Uuid.generateV7().toJavaUuid()
}
