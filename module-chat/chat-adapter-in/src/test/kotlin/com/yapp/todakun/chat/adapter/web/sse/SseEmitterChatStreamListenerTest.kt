package com.yapp.todakun.chat.adapter.web.sse

import com.yapp.todakun.chat.ChatAction
import com.yapp.todakun.chat.ChatActionType
import com.yapp.todakun.chat.exception.ChatConversationForbiddenException
import com.yapp.todakun.chat.port.inbound.ChatTurnStarted
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import java.util.function.Consumer

private val CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private val USER_MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private val ASSISTANT_MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000003")

class SseEmitterChatStreamListenerTest :
    DescribeSpec({
        val emitter = mockk<SseEmitter>()
        val onCompletionSlot = slot<Runnable>()
        val onTimeoutSlot = slot<Runnable>()
        val onErrorSlot = slot<Consumer<Throwable>>()

        fun createListener(): SseEmitterChatStreamListener {
            every { emitter.onCompletion(capture(onCompletionSlot)) } just Runs
            every { emitter.onTimeout(capture(onTimeoutSlot)) } just Runs
            every { emitter.onError(capture(onErrorSlot)) } just Runs
            return SseEmitterChatStreamListener(emitter)
        }

        fun stubSend(): CapturingSlot<SseEmitter.SseEventBuilder> {
            val builderSlot = slot<SseEmitter.SseEventBuilder>()
            every { emitter.send(capture(builderSlot)) } just Runs
            return builderSlot
        }

        fun stubComplete() {
            every { emitter.complete() } just Runs
        }

        fun sentEvent(builder: SseEmitter.SseEventBuilder): Pair<String, Any?> {
            val parts = builder.build()
            val meta = parts.first { it.data is String }.data as String
            val payload = parts.first { it.data !is String }.data
            val eventName = Regex("event:(\\S+)").find(meta)!!.groupValues[1]
            return eventName to payload
        }

        afterTest { clearMocks(emitter) }

        describe("onStart") {
            context("호출되면") {
                it("start 이벤트로 대화/메시지 ID와 쿼터 정보를 전달한다") {
                    val listener = createListener()
                    val builderSlot = stubSend()
                    val started = ChatTurnStarted(CONVERSATION_ID, USER_MESSAGE_ID, ASSISTANT_MESSAGE_ID, quotaUsed = 1, quotaLimit = 3)

                    listener.onStart(started)

                    val (eventName, payload) = sentEvent(builderSlot.captured)
                    eventName shouldBe "start"
                    payload shouldBe ChatStartEvent.from(started)
                }
            }
        }

        describe("onDelta") {
            context("호출되면") {
                it("delta 이벤트로 텍스트 조각을 전달한다") {
                    val listener = createListener()
                    val builderSlot = stubSend()

                    listener.onDelta("안녕하세요")

                    val (eventName, payload) = sentEvent(builderSlot.captured)
                    eventName shouldBe "delta"
                    payload shouldBe ChatDeltaEvent("안녕하세요")
                }
            }
        }

        describe("onAction") {
            context("호출되면") {
                it("action 이벤트로 액션 카드를 전달한다") {
                    val listener = createListener()
                    val builderSlot = stubSend()
                    val action = ChatAction(ChatActionType.CALENDAR_ADD, "내 캘린더에 추가하기", "계약・이사", LocalDate.of(2026, 7, 25))

                    listener.onAction(action)

                    val (eventName, payload) = sentEvent(builderSlot.captured)
                    eventName shouldBe "action"
                    payload shouldBe ChatActionEvent.from(action)
                }
            }
        }

        describe("onDone") {
            context("호출되면") {
                it("done 이벤트를 보내고 emitter를 완료한다") {
                    val listener = createListener()
                    val builderSlot = stubSend()
                    stubComplete()

                    listener.onDone(ASSISTANT_MESSAGE_ID)

                    val (eventName, payload) = sentEvent(builderSlot.captured)
                    eventName shouldBe "done"
                    payload shouldBe ChatDoneEvent(ASSISTANT_MESSAGE_ID)
                    verify(exactly = 1) { emitter.complete() }
                }
            }
        }

        describe("onError") {
            context("BusinessException이면") {
                it("해당 errorCode의 code/message로 error 이벤트를 보내고 완료한다") {
                    val listener = createListener()
                    val builderSlot = stubSend()
                    stubComplete()

                    listener.onError(ChatConversationForbiddenException())

                    val (eventName, payload) = sentEvent(builderSlot.captured)
                    eventName shouldBe "error"
                    payload shouldBe ChatErrorEvent("CHAT-403", "해당 대화에 접근할 권한이 없습니다.")
                    verify(exactly = 1) { emitter.complete() }
                }
            }

            context("BusinessException이 아니면") {
                it("CHAT-500과 기본 오류 메시지로 error 이벤트를 보내고 완료한다") {
                    val listener = createListener()
                    val builderSlot = stubSend()
                    stubComplete()

                    listener.onError(RuntimeException("알 수 없는 오류"))

                    val (eventName, payload) = sentEvent(builderSlot.captured)
                    eventName shouldBe "error"
                    payload shouldBe ChatErrorEvent("CHAT-500", "일시적인 오류가 발생했습니다.")
                    verify(exactly = 1) { emitter.complete() }
                }
            }
        }

        describe("연결 종료 콜백") {
            context("emitter의 onCompletion 콜백이 호출되면") {
                it("isClientConnected가 false로 바뀌고 이후 이벤트를 보내지 않는다") {
                    val listener = createListener()

                    onCompletionSlot.captured.run()

                    listener.isClientConnected() shouldBe false
                    listener.onDelta("전송되면 안 됨")
                    verify(exactly = 0) { emitter.send(any<SseEmitter.SseEventBuilder>()) }
                }
            }

            context("emitter의 onTimeout 콜백이 호출되면") {
                it("isClientConnected가 false로 바뀐다") {
                    val listener = createListener()

                    onTimeoutSlot.captured.run()

                    listener.isClientConnected() shouldBe false
                }
            }

            context("emitter의 onError 콜백이 호출되면") {
                it("isClientConnected가 false로 바뀐다") {
                    val listener = createListener()

                    onErrorSlot.captured.accept(IOException("연결 끊김"))

                    listener.isClientConnected() shouldBe false
                }
            }
        }

        describe("emitter.send 실패 처리") {
            context("IOException이 발생하면") {
                it("연결 끊김으로 처리하고 예외를 전파하지 않는다") {
                    val listener = createListener()
                    every { emitter.send(any<SseEmitter.SseEventBuilder>()) } throws IOException("클라이언트 연결 종료")

                    listener.onDelta("텍스트")

                    listener.isClientConnected() shouldBe false
                }
            }

            context("IllegalStateException이 발생하면") {
                it("연결 끊김으로 처리하고 예외를 전파하지 않는다") {
                    val listener = createListener()
                    every {
                        emitter.send(any<SseEmitter.SseEventBuilder>())
                    } throws IllegalStateException("이미 완료된 emitter")

                    listener.onDelta("텍스트")

                    listener.isClientConnected() shouldBe false
                }
            }

            context("그 외 예외가 발생하면") {
                it("연결 끊김으로 처리하고 예외를 전파하지 않는다") {
                    val listener = createListener()
                    every {
                        emitter.send(any<SseEmitter.SseEventBuilder>())
                    } throws RuntimeException("직렬화 실패")

                    listener.onDelta("텍스트")

                    listener.isClientConnected() shouldBe false
                }
            }
        }

        describe("isClientConnected") {
            context("연결이 끊기기 전이면") {
                it("true를 반환한다") {
                    val listener = createListener()

                    listener.isClientConnected() shouldBe true
                }
            }
        }
    })
