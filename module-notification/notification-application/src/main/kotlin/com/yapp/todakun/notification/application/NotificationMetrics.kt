package com.yapp.todakun.notification.application

import com.yapp.todakun.shared.NotificationType
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

private const val METRIC_NAME = "notification.dispatch"

/**
 * 알림 발송 결과 카운터(이 저장소 첫 커스텀 Micrometer 지표). `/actuator/prometheus`로 자동 노출되어
 * 기존 Grafana Alloy 스크레이핑 경로에서 성공/실패/재시도 소진율을 볼 수 있다.
 * `errorCode`는 실패 원인(FCM MessagingErrorCode 등)별 추이를 볼 수 있도록 태그로 남긴다.
 * 값이 고정된 소수 집합(~9종)이라 카디널리티 문제는 없다. 성공 시엔 호출부가 "NONE"을 명시적으로 넘긴다.
 */
@Component
class NotificationMetrics(
    private val meterRegistry: MeterRegistry,
) {
    fun record(
        type: NotificationType,
        result: NotificationDispatchResult,
        errorCode: String,
    ) {
        meterRegistry.counter(METRIC_NAME, "type", type.name, "result", result.label, "error_code", errorCode).increment()
    }

    companion object {
        /** 성공이라 실패 원인이 없을 때 호출부가 명시적으로 넘기는 값. */
        const val ERROR_CODE_NONE = "NONE"

        /** 실패했지만 FCM 에러코드를 알 수 없을 때(배치 호출 자체 예외 등) 호출부가 넘기는 폴백 값. */
        const val ERROR_CODE_UNKNOWN = "UNKNOWN"
    }
}

enum class NotificationDispatchResult(
    val label: String,
) {
    SUCCESS("success"),
    FAILURE("failure"),
    RETRY_EXHAUSTED("retry_exhausted"),
}
