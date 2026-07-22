package com.yapp.todakun.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** @Scheduled 스케줄러(알림 발송 등)를 활성화한다. */
@Configuration
@EnableScheduling
class SchedulingConfig
