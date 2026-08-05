package com.yapp.todakun.dailyfortune.application.batch

import com.yapp.todakun.shared.GetMemberIdsPort
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.infrastructure.item.ExecutionContext
import org.springframework.batch.infrastructure.item.ItemStreamReader
import java.util.UUID

private const val AFTER_MEMBER_ID_KEY = "memberIdItemReader.afterMemberId"

/**
 * keyset 페이징으로 전체 회원 ID를 순회하는 ItemReader.
 * [afterMemberId]를 매 아이템마다 갱신해 ExecutionContext에 저장하므로, Job 재시작 시 마지막으로 커밋된 회원 다음부터 이어서 읽는다.
 * 실제 스텝 스코프는 [GenerateDailyFortunesJobConfig]의 `@Bean` 메서드에 붙은 `@StepScope`가 부여한다.
 * 클래스 레벨의 [StepScope]는 Spring 런타임에는 영향이 없고, kotlin-allopen이 CGLIB 프록시용으로 이 클래스를 open 처리하도록 컴파일 타임에 표시하는 용도다.
 */
@StepScope
class MemberIdItemReader(
    private val getMemberIdsPort: GetMemberIdsPort,
    private val pageSize: Int,
) : ItemStreamReader<UUID> {
    private var afterMemberId: UUID? = null
    private var buffer: Iterator<UUID> = emptyList<UUID>().iterator()
    private var exhausted = false

    override fun read(): UUID? {
        if (!(buffer.hasNext() || refillBuffer())) return null

        return buffer.next().also { afterMemberId = it }
    }

    private fun refillBuffer(): Boolean {
        if (exhausted) return false

        val page = getMemberIdsPort.getMemberIds(afterMemberId, pageSize)
        exhausted = page.isEmpty()
        buffer = page.iterator()

        return !exhausted
    }

    override fun open(executionContext: ExecutionContext) {
        if (executionContext.containsKey(AFTER_MEMBER_ID_KEY)) {
            afterMemberId = UUID.fromString(executionContext.getString(AFTER_MEMBER_ID_KEY))
        }
    }

    override fun update(executionContext: ExecutionContext) {
        afterMemberId?.let { executionContext.putString(AFTER_MEMBER_ID_KEY, it.toString()) }
    }

    override fun close() = Unit
}
