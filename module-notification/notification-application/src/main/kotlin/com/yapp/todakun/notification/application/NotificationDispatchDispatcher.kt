package com.yapp.todakun.notification.application

import kotlinx.coroutines.Dispatchers

// prod/dev/local 모두 datasource.hikari.maximum-pool-size를 명시하지 않아 기본값(10)을 쓰고,
// 이 풀은 웹 요청 스레드와도 공유된다. NotificationDispatchService/RetryFailedNotificationsService가
// 각자 별도 상한을 두면 합산 동시성이 커넥션 풀을 넘어설 수 있어(예: 관리자 API로 트리거되는
// publish가 스케줄 발송과 겹치는 경우), 두 서비스가 이 인스턴스 하나를 공유해 전체 동시성을 상한 4로 묶는다.
val notificationDispatchDispatcher = Dispatchers.IO.limitedParallelism(4)
