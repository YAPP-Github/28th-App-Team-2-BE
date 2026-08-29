package com.yapp.todakun.shared.event

import java.util.UUID

/**
 * 신규 회원의 가입(회원·본인 사주 명식 생성) 트랜잭션이 커밋되어,
 * 가입 이후 후속 처리(당일 운세 생성 등)를 시작해도 안전해졌음을 알리는 도메인 이벤트(auth-application에서 발행).
 * 향후 RabbitMQ 등 메시지 브로커 도입 시 발행부 코드 변경 없이 구독 방식만 교체할 수 있도록,
 * 지금은 Spring `ApplicationEventPublisher` + `@TransactionalEventListener`로 인프로세스 발행/구독한다.
 */
data class MemberSignedUpEvent(
    val memberId: UUID,
)
