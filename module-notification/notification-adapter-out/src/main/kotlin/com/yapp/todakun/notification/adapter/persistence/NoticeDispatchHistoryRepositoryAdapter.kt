package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.notification.NoticeDispatchHistory
import com.yapp.todakun.notification.port.outbound.NoticeDispatchHistoryRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

/**
 * `@Transactional`을 의도적으로 붙이지 않는다 — [jpaRepository]의 각 호출이 독립된 짧은 트랜잭션으로 실행돼야,
 * 유니크 제약 위반([DataIntegrityViolationException])이 나더라도 바깥 트랜잭션이 rollback-only로 오염되지 않는다.
 */
@Repository
@Loggable
class NoticeDispatchHistoryRepositoryAdapter(
    private val jpaRepository: NoticeDispatchHistoryJpaRepository,
) : NoticeDispatchHistoryRepository {
    override fun saveIfAbsent(history: NoticeDispatchHistory): Boolean {
        if (jpaRepository.existsByIdempotencyKey(history.idempotencyKey)) return false
        return try {
            jpaRepository.save(NoticeDispatchHistoryJpaEntity.fromDomain(history))
            true
        } catch (e: DataIntegrityViolationException) {
            // 선조회를 통과한 동시 실행 2건이 동시에 INSERT한 경우 — 유니크 제약이 걸러낸 쪽은 이미 처리된 것으로 본다.
            log.info("동시 실행으로 인한 유니크 제약 위반이라 이미 처리된 공지로 간주합니다: idempotencyKey=${history.idempotencyKey}")
            false
        }
    }
}
