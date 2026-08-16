package com.yapp.todakun.notification.adapter.runner

import com.yapp.todakun.notification.NoticeType
import com.yapp.todakun.notification.port.inbound.PublishNoticeCommand
import com.yapp.todakun.notification.port.inbound.PublishNoticeUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.boot.DefaultApplicationArguments

class NoticePublishRunnerTest :
    DescribeSpec(
        {
            val publishNoticeUseCase = mockk<PublishNoticeUseCase>()
            val runner = NoticePublishRunner(publishNoticeUseCase)

            afterTest { clearMocks(publishNoticeUseCase) }

            describe("run") {
                context("notice.title/notice.content 인자가 모두 있으면") {
                    it("해당 값으로 공지를 발행한다") {
                        every { publishNoticeUseCase.publish(any()) } returns Unit
                        val args =
                            DefaultApplicationArguments(
                                "--notice.title=제목",
                                "--notice.content=내용",
                                "--notice.deep-link=notice/1",
                            )

                        runner.run(args)

                        verify(exactly = 1) {
                            publishNoticeUseCase.publish(PublishNoticeCommand("제목", "내용", "notice/1", NoticeType.GENERAL, null))
                        }
                    }
                }

                context("notice.type 인자가 없으면") {
                    it("타입은 GENERAL로 기본 처리된다") {
                        val captured = slot<PublishNoticeCommand>()
                        every { publishNoticeUseCase.publish(capture(captured)) } returns Unit
                        val args = DefaultApplicationArguments("--notice.title=제목", "--notice.content=내용")

                        runner.run(args)

                        captured.captured.type shouldBe NoticeType.GENERAL
                    }
                }

                context("notice.type 인자가 있으면") {
                    it("해당 타입이 커맨드에 그대로 전달된다") {
                        val captured = slot<PublishNoticeCommand>()
                        every { publishNoticeUseCase.publish(capture(captured)) } returns Unit
                        val args =
                            DefaultApplicationArguments("--notice.title=제목", "--notice.content=내용", "--notice.type=EVENT")

                        runner.run(args)

                        captured.captured.type shouldBe NoticeType.EVENT
                    }
                }

                context("notice.idempotency-key 인자가 있으면") {
                    it("커맨드에 그대로 전달된다") {
                        val captured = slot<PublishNoticeCommand>()
                        every { publishNoticeUseCase.publish(capture(captured)) } returns Unit
                        val args =
                            DefaultApplicationArguments(
                                "--notice.title=제목",
                                "--notice.content=내용",
                                "--notice.idempotency-key=notice-2026-08-12-01",
                            )

                        runner.run(args)

                        captured.captured.idempotencyKey shouldBe "notice-2026-08-12-01"
                    }
                }

                context("notice.idempotency-key 인자가 없으면") {
                    it("커맨드의 idempotencyKey는 null이다") {
                        val captured = slot<PublishNoticeCommand>()
                        every { publishNoticeUseCase.publish(capture(captured)) } returns Unit
                        val args =
                            DefaultApplicationArguments(
                                "--notice.title=제목",
                                "--notice.content=내용",
                            )

                        runner.run(args)

                        captured.captured.idempotencyKey.shouldBeNull()
                    }
                }

                context("notice.title/notice.content 인자가 없으면") {
                    it("공지를 발행하지 않는다(평상시 서버 기동에 영향 없음)") {
                        val args = DefaultApplicationArguments()

                        runner.run(args)

                        verify(exactly = 0) { publishNoticeUseCase.publish(any()) }
                    }
                }
            }
        },
    )
