package com.yapp.todakun.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import org.springframework.ai.util.LoggingMarkers

/**
 * spring-ai가 원문 텍스트(사용자 입력 포함, 예: `BeanOutputConverter` 파싱 실패)를 찍는 ERROR에 붙이는
 * [LoggingMarkers.SENSITIVE_DATA_MARKER]를 보고 해당 이벤트만 걸러낸다.
 *
 * logback-classic에는 appender에 붙일 수 있는 마커 기반 `Filter`가 없다(`classic.turbo.MarkerFilter`는
 * 컨텍스트 전역 TurboFilter라 특정 appender만 골라 막을 수 없다). DISCORD appender에만 이 필터를 붙여
 * 재시도로 자연 복구되는 파싱 실패까지 팀 채널로 새는 걸 막되, 다른 appender(콘솔·Sentry)는 그대로 둬
 * Loki·Sentry에는 원문이 남게 한다.
 */
class SensitiveMarkerFilter : Filter<ILoggingEvent>() {
    override fun decide(event: ILoggingEvent): FilterReply =
        if (event.markerList.orEmpty().any { it.contains(LoggingMarkers.SENSITIVE_DATA_MARKER) }) FilterReply.DENY else FilterReply.NEUTRAL
}
