package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.notification.NoticeDispatchHistory
import com.yapp.todakun.notification.port.outbound.NoticeDispatchHistoryRepository
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

// NoticeDispatchHistoryJpaEntity에 선언한 idempotency_key 유니크 제약 이름.
private const val IDEMPOTENCY_KEY_CONSTRAINT = "uk_notice_dispatch_history_idempotency_key"

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
            // save가 아니라 saveAndFlush다 — 선점 성공 여부는 호출자가 팬아웃을 시작하기 전에 확정돼야 한다.
            // flush를 미루면 제약 위반이 트랜잭션 커밋 시점에야 터져, 이미 발송을 마친 뒤에 실패를 알게 된다.
            jpaRepository.saveAndFlush(NoticeDispatchHistoryJpaEntity.fromDomain(history))
            true
        } catch (e: DataIntegrityViolationException) {
            // 멱등키 유니크 제약 위반만 "이미 처리됨"으로 해석한다 — 선조회를 통과한 동시 실행 2건이 동시에 INSERT한 경우다.
            // 제목 길이 초과 등 다른 무결성 위반까지 false로 뭉개면, 공지가 발송되지 않았는데도 "이미 발송됨"으로
            // 스킵 로그만 남고 조용히 사라진다.
            if (!isIdempotencyKeyConflict(e)) throw e
            log.info("동시 실행으로 인한 멱등키 유니크 제약 위반이라 이미 처리된 공지로 간주합니다: idempotencyKey=${history.idempotencyKey}")
            false
        }
    }
}

/**
 * 예외 원인 사슬에 [IDEMPOTENCY_KEY_CONSTRAINT] 위반이 있는지 확인한다.
 * 원인이 자기 자신을 가리키는 예외에 걸려 무한 순회하지 않도록 자기 참조에서 멈춘다.
 */
internal fun isIdempotencyKeyConflict(exception: Throwable): Boolean =
    generateSequence(exception) { current -> current.cause.takeIf { it !== current } }
        .filterIsInstance<ConstraintViolationException>()
        .any { IDEMPOTENCY_KEY_CONSTRAINT.equals(it.constraintName, ignoreCase = true) }
