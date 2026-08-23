package com.yapp.todakun.auth.application.listener

import com.yapp.todakun.auth.application.config.SignupDailyFortuneAsyncConfig.Companion.SIGNUP_DAILY_FORTUNE_EXECUTOR_BEAN_NAME
import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.currentDate
import com.yapp.todakun.shared.event.MemberSignedUpEvent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * [MemberSignedUpEvent]를 받아 가입 직후 당일 운세를 생성한다. `AFTER_COMMIT`이라 회원·사주 명식이 실제로 커밋된 뒤에만 실행된다.
 * 리스너 메서드 자체는 실제 AI 호출을 전용 워커 스레드([SignupDailyFortuneAsyncConfig])에 위임하고 즉시 반환해, 커밋을 수행한 요청 스레드를 붙잡지 않는다.
 * 신규 가입자는 이전 서비스 데이의 연속성을 지킬 필요가 없으므로 롤오버 규칙 대신 실제 캘린더 날짜를 쓴다.
 * 실패해도 이미 확정된 가입을 되돌리지 않는다 — 최초 조회(GetTodayFortuneService)나 다음 배치가 채운다.
 */
@Service
@Loggable
class MemberSignedUpEventListener(
    private val createDailyFortunePort: CreateDailyFortunePort,
    @Qualifier(SIGNUP_DAILY_FORTUNE_EXECUTOR_BEAN_NAME)
    private val signupDailyFortuneTaskExecutor: AsyncTaskExecutor,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onMemberSignedUp(event: MemberSignedUpEvent) {
        try {
            signupDailyFortuneTaskExecutor.execute {
                try {
                    createDailyFortunePort.create(event.memberId, currentDate())
                } catch (e: Exception) {
                    log.warn("가입 직후 오늘의 운세 생성 실패(가입은 유지, 최초 조회 시 재생성): memberId={}", event.memberId, e)
                }
            }
        } catch (e: TaskRejectedException) {
            // 워커 풀이 포화되어 작업 자체를 받아들이지 못한 경우(큐 가득 참) — 최초 조회 시 재생성 경로로 흡수한다.
            log.warn("전용 워커 풀 포화로 가입 직후 오늘의 운세 생성을 큐에 넣지 못함(최초 조회 시 재생성): memberId={}", event.memberId, e)
        }
    }
}
