package com.yapp.todakun.notification

import com.yapp.todakun.shared.NotificationType
import java.time.LocalTime
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 회원별 알림 설정(알림 설정 화면의 토글 3종).
 * - morningReport: 아침 운 리포트(받을 시간 지정, 기본 08:00, 30분 단위)
 * - todaki: 토닥이 답변 완료 알림(이벤트)
 * - luckyActionReminder: 행운 액션 리마인드(매일 20:00 고정 → 시각 필드 없음)
 * 설정 화면 default가 전부 OFF이므로 기본값은 모두 false.
 */
data class NotificationSetting(
    val id: UUID,
    val memberId: UUID,
    val morningReportEnabled: Boolean,
    val morningReportTime: LocalTime,
    val todakiEnabled: Boolean,
    val luckyActionReminderEnabled: Boolean,
) {
    fun update(
        morningReportEnabled: Boolean,
        morningReportTime: LocalTime,
        todakiEnabled: Boolean,
        luckyActionReminderEnabled: Boolean,
    ): NotificationSetting =
        copy(
            morningReportEnabled = morningReportEnabled,
            // 휠피커가 30분 단위(00/30)이므로 스케줄러 매칭을 위해 반시각으로 스냅한다.
            morningReportTime = morningReportTime.snapToHalfHour(),
            todakiEnabled = todakiEnabled,
            luckyActionReminderEnabled = luckyActionReminderEnabled,
        )

    /** 알림 종류별 수신 토글. NOTICE(공지)는 항상 수신. */
    fun isPushEnabledFor(type: NotificationType): Boolean =
        when (type) {
            NotificationType.FORTUNE -> morningReportEnabled
            NotificationType.AI_COMPLETE -> todakiEnabled
            NotificationType.LUCKY_ACTION -> luckyActionReminderEnabled
            NotificationType.NOTICE -> true
        }

    companion object {
        @ExperimentalUuidApi
        fun createDefault(memberId: UUID): NotificationSetting =
            NotificationSetting(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                morningReportEnabled = false,
                // 기본 아침 리포트 시각
                morningReportTime = LocalTime.of(8, 0),
                todakiEnabled = false,
                luckyActionReminderEnabled = false,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            morningReportEnabled: Boolean,
            morningReportTime: LocalTime,
            todakiEnabled: Boolean,
            luckyActionReminderEnabled: Boolean,
        ): NotificationSetting =
            NotificationSetting(
                id,
                memberId,
                morningReportEnabled,
                morningReportTime,
                todakiEnabled,
                luckyActionReminderEnabled,
            )
    }
}

private fun LocalTime.snapToHalfHour(): LocalTime = LocalTime.of(hour, if (minute < 30) 0 else 30)
