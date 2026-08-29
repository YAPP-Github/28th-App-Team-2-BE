package com.yapp.todakun.notification.port.inbound

/**
 * 공지(NOTICE) 발송 유스케이스. 전체 회원에게 fan-out 발송한다.
 * 두 진입점이 이 유스케이스를 공유한다: 운영 스크립트([com.yapp.todakun.notification.adapter.runner.NoticePublishRunner],
 * 서버 기동 인자로 1회성 실행)와 관리자 REST API(`POST /api/v1/admin/notifications/notice`, ROLE_ADMIN 인가).
 * [PublishNoticeCommand.idempotencyKey](또는 그 부재 시 제목·본문·딥링크에서 파생한 키)로 처리 이력을 남겨,
 * 같은 인자로 반복 호출해도 실제 발송은 최초 1회만 일어나는 멱등성을 보장한다.
 */
interface PublishNoticeUseCase {
    fun publish(command: PublishNoticeCommand)
}
