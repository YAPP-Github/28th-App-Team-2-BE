package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.time.LocalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class NotificationSettingRepositoryAdapterTest(
    private val notificationSettingJpaRepository: NotificationSettingJpaRepository,
) : DescribeSpec(
        {
            val adapter = NotificationSettingRepositoryAdapter(notificationSettingJpaRepository)

            fun morningReportSetting(time: LocalTime = LocalTime.of(8, 0)) =
                NotificationSetting
                    .createDefault(Uuid.generateV7().toJavaUuid())
                    .update(
                        morningReportEnabled = true,
                        morningReportTime = time,
                        todakiEnabled = false,
                        luckyActionReminderEnabled = false,
                    )

            fun luckyActionSetting() =
                NotificationSetting
                    .createDefault(Uuid.generateV7().toJavaUuid())
                    .update(
                        morningReportEnabled = false,
                        morningReportTime = LocalTime.of(8, 0),
                        todakiEnabled = false,
                        luckyActionReminderEnabled = true,
                    )

            describe("save") {
                context("OS 알림 권한 동기화 값을 저장하면") {
                    it("조회 시 그대로 복원된다") {
                        val setting = NotificationSetting.createDefault(Uuid.generateV7().toJavaUuid())

                        val saved = adapter.save(setting.syncOsPushPermission(false))
                        val found = adapter.findByMemberId(saved.memberId)

                        found?.osPushPermission shouldBe false
                    }
                }

                context("OS 알림 권한을 동기화한 적이 없으면") {
                    it("null로 조회된다") {
                        val saved = adapter.save(NotificationSetting.createDefault(Uuid.generateV7().toJavaUuid()))

                        val found = adapter.findByMemberId(saved.memberId)

                        found?.osPushPermission shouldBe null
                    }
                }
            }

            describe("findMorningReportTargets") {
                context("대상이 페이지 크기보다 많으면") {
                    it("afterId 커서로 다음 페이지를 이어서 조회할 수 있다") {
                        val saved = (1..3).map { adapter.save(morningReportSetting()) }
                        // 받을 시간이 다른 설정은 대상에서 제외된다.
                        adapter.save(morningReportSetting(time = LocalTime.of(9, 0)))

                        val firstPage = adapter.findMorningReportTargets(LocalTime.of(8, 0), afterId = null, limit = 2)
                        val secondPage = adapter.findMorningReportTargets(LocalTime.of(8, 0), afterId = firstPage.last().id, limit = 2)

                        firstPage.size shouldBe 2
                        (firstPage + secondPage).map { it.id } shouldContainExactlyInAnyOrder saved.map { it.id }
                    }
                }
            }

            describe("findLuckyActionReminderTargets") {
                context("행운 액션 리마인드가 꺼진 설정은") {
                    it("대상에서 제외된다") {
                        val enabled = adapter.save(luckyActionSetting())
                        adapter.save(morningReportSetting())

                        val targets = adapter.findLuckyActionReminderTargets(afterId = null, limit = 100)

                        targets.map { it.id } shouldContainExactlyInAnyOrder listOf(enabled.id)
                    }
                }
            }
        },
    )
