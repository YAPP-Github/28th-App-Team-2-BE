package com.yapp.todakun.shared

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
private const val SERVICE_DAY_CUTOVER_HOUR = 6

/** 오늘의 운세 서비스 일자. 06:00 미만이면 전날을, 06:00 이상이면 당일을 "오늘"로 간주한다. */
fun currentFortuneServiceDate(now: LocalDateTime = LocalDateTime.now(SEOUL_ZONE)): LocalDate =
    if (now.hour < SERVICE_DAY_CUTOVER_HOUR) now.toLocalDate().minusDays(1) else now.toLocalDate()
