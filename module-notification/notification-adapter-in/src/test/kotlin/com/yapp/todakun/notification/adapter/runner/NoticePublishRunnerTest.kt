package com.yapp.todakun.notification.adapter.runner

import com.yapp.todakun.notification.port.inbound.PublishNoticeUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
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
                        every { publishNoticeUseCase.publish(any(), any(), any()) } returns Unit
                        val args =
                            DefaultApplicationArguments(
                                "--notice.title=제목",
                                "--notice.content=내용",
                                "--notice.deep-link=notice/1",
                            )

                        runner.run(args)

                        verify(exactly = 1) { publishNoticeUseCase.publish("제목", "내용", "notice/1") }
                    }
                }

                context("notice.title/notice.content 인자가 없으면") {
                    it("공지를 발행하지 않는다(평상시 서버 기동에 영향 없음)") {
                        val args = DefaultApplicationArguments()

                        runner.run(args)

                        verify(exactly = 0) { publishNoticeUseCase.publish(any(), any(), any()) }
                    }
                }
            }
        },
    )
