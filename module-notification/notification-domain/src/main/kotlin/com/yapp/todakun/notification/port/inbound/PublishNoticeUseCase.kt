package com.yapp.todakun.notification.port.inbound

/**
 * 공지(NOTICE) 발송 유스케이스. 전체 회원에게 fan-out 발송한다.
 * 두 진입점이 이 유스케이스를 공유한다: 운영 스크립트([com.yapp.todakun.notification.adapter.runner.NoticePublishRunner],
 * 서버 기동 인자로 1회성 실행)와 관리자 REST API(`POST /api/v1/admin/notifications/notice`, ROLE_ADMIN 인가).
 */
interface PublishNoticeUseCase {
    fun publish(
        title: String,
        content: String,
        deepLink: String?,
    )
}
