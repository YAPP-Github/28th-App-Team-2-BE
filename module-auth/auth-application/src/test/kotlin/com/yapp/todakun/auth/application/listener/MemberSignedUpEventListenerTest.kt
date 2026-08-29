package com.yapp.todakun.auth.application.listener

import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.event.MemberSignedUpEvent
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.TaskRejectedException
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private val MEMBER_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000010")

@ExperimentalUuidApi
class MemberSignedUpEventListenerTest :
    DescribeSpec(
        {
            val createDailyFortunePort = mockk<CreateDailyFortunePort>()
            val signupDailyFortuneTaskExecutor = mockk<AsyncTaskExecutor>()
            val listener = MemberSignedUpEventListener(createDailyFortunePort, signupDailyFortuneTaskExecutor)

            afterTest { clearMocks(createDailyFortunePort, signupDailyFortuneTaskExecutor) }

            val event = MemberSignedUpEvent(MEMBER_ID)

            describe("onMemberSignedUp") {
                context("전용 워커에 작업 등록까지는 성공하면") {
                    it("리스너 스레드가 아니라 워커가 실행할 때 오늘의 운세 생성을 위임한다") {
                        val taskSlot = slot<Runnable>()
                        every { signupDailyFortuneTaskExecutor.execute(capture(taskSlot)) } just Runs

                        listener.onMemberSignedUp(event)

                        // execute()에 등록만 됐을 뿐 아직 실행 전이라, 워커가 돌기 전에는 AI가 호출되지 않는다.
                        verify(exactly = 0) { createDailyFortunePort.create(any(), any()) }

                        every { createDailyFortunePort.create(MEMBER_ID, any()) } returns Uuid.generateV7().toJavaUuid()
                        taskSlot.captured.run()

                        verify(exactly = 1) { createDailyFortunePort.create(MEMBER_ID, any()) }
                    }
                }

                context("워커에 위임한 생성이 실패해도") {
                    it("예외를 삼키고 워커 스레드 밖으로 전파하지 않는다") {
                        val taskSlot = slot<Runnable>()
                        every { signupDailyFortuneTaskExecutor.execute(capture(taskSlot)) } just Runs
                        every { createDailyFortunePort.create(MEMBER_ID, any()) } throws IllegalStateException("AI 호출 실패")

                        listener.onMemberSignedUp(event)

                        shouldNotThrowAny { taskSlot.captured.run() }
                    }
                }

                context("전용 워커 풀이 포화 상태라 작업 등록 자체가 거부되면") {
                    it("TaskRejectedException을 삼키고 AI를 호출하지 않는다") {
                        every { signupDailyFortuneTaskExecutor.execute(any()) } throws TaskRejectedException("큐 가득 참")

                        shouldNotThrowAny { listener.onMemberSignedUp(event) }

                        verify(exactly = 0) { createDailyFortunePort.create(any(), any()) }
                    }
                }
            }
        },
    )
