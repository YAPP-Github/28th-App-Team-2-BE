package com.yapp.todakun.notification.application

import com.yapp.todakun.shared.NotificationType
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

private const val METRIC_NAME = "notification.dispatch"

/**
 * 알림 발송 결과 카운터(이 저장소 첫 커스텀 Micrometer 지표). `/actuator/prometheus`로 자동 노출되어
 * 기존 Grafana Alloy 스크레이핑 경로에서 성공/실패/재시도 소진율을 볼 수 있다.
 */
@Component
class NotificationMetrics(
    private val meterRegistry: MeterRegistry,
) {
    fun record(
        type: NotificationType,
        result: NotificationDispatchResult,
    ) {
        meterRegistry.counter(METRIC_NAME, "type", type.name, "result", result.label).increment()
    }
}

enum class NotificationDispatchResult(
    val label: String,
) {
    SUCCESS("success"),
    FAILURE("failure"),
    RETRY_EXHAUSTED("retry_exhausted"),
}
