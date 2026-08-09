package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalTime
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class NotificationSettingRepositoryAdapterTest(
    private val notificationSettingJpaRepository: NotificationSettingJpaRepository,
    transactionManager: PlatformTransactionManager,
) : DescribeSpec(
        {
            val adapter = NotificationSettingRepositoryAdapter(notificationSettingJpaRepository)
            // 워커 스레드는 테스트 트랜잭션(메인 스레드 ThreadLocal)에 접근할 수 없어, @Modifying 쿼리를
            // 실행하려면 스레드마다 독립된 새 트랜잭션이 필요하다.
            val transactionTemplate = TransactionTemplate(transactionManager)

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

                context("같은 회원에 대해 최초 생성 요청이 동시에 들어오면(두 기기에서 동시 동기화)") {
                    it("예외 없이 정확히 한 행만 남는다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val barrier = CyclicBarrier(2)
                        val executor = Executors.newFixedThreadPool(2)

                        try {
                            val futures =
                                listOf(true, false).map { granted ->
                                    executor.submit {
                                        barrier.await(10, TimeUnit.SECONDS)
                                        transactionTemplate.execute {
                                            adapter.save(NotificationSetting.createDefault(memberId).syncOsPushPermission(granted))
                                        }
                                    }
                                }
                            futures.forEach { it.get(10, TimeUnit.SECONDS) }

                            notificationSettingJpaRepository.findByMemberId(memberId).shouldNotBeNull()
                        } finally {
                            executor.shutdown()
                        }
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
