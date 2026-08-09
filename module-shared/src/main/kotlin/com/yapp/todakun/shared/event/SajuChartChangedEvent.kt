package com.yapp.todakun.shared.event

import java.util.UUID

/**
 * 회원의 사주 명식이 재계산(교체)되거나 삭제되어, 그 명식에 파생된 다른 도메인의 캐시·데이터가
 * 더 이상 유효하지 않게 됐음을 알리는 도메인 이벤트(saju-application에서 발행).
 * 향후 RabbitMQ 등 메시지 브로커 도입 시 발행부 코드 변경 없이 구독 방식만 교체할 수 있도록,
 * 지금은 Spring `ApplicationEventPublisher` + `@TransactionalEventListener`로 인프로세스 발행/구독한다.
 */
data class SajuChartChangedEvent(
    val memberId: UUID,
)
