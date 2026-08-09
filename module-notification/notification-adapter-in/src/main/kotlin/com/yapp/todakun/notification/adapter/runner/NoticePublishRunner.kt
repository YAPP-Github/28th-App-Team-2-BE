package com.yapp.todakun.notification.adapter.runner

import com.yapp.todakun.notification.port.inbound.PublishNoticeUseCase
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

private const val NOTICE_TITLE_ARG = "notice.title"
private const val NOTICE_CONTENT_ARG = "notice.content"
private const val NOTICE_DEEP_LINK_ARG = "notice.deep-link"

/**
 * 공지(NOTICE) 발송 운영 스크립트 진입점.
 * `--notice.title`/`--notice.content`(둘 다 있을 때만) 인자가 있으면 공지를 1회 발행하고,
 * 없으면 즉시 no-op이라 평상시 서버 기동에는 영향이 없다.
 * 관리자 API·역할 기반 인가를 새로 만들지 않는다 — 이 인자를 넣어 앱을 기동할 수 있는 사람
 * (배포/서버 접근 권한 보유자)만 실행할 수 있다는 기존 운영 신뢰 경계를 그대로 사용한다.
 * 상시 구동 중인 서버의 정기 재배포 파이프라인에 이 인자를 상시로 넣지 말 것 — 재기동마다 중복 발송된다.
 * 운영자가 별도로 1회성 기동(로컬/배스천에서 동일 이미지·profile로 1회 실행)할 때만 사용한다.
 */
@Component
class NoticePublishRunner(
    private val publishNoticeUseCase: PublishNoticeUseCase,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val title = args.getOptionValues(NOTICE_TITLE_ARG)?.firstOrNull() ?: return
        val content = args.getOptionValues(NOTICE_CONTENT_ARG)?.firstOrNull() ?: return
        val deepLink = args.getOptionValues(NOTICE_DEEP_LINK_ARG)?.firstOrNull()

        publishNoticeUseCase.publish(title, content, deepLink)
    }
}
