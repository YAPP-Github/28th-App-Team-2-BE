package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.notification.Notification
import com.yapp.todakun.notification.config.TestContainersConfig
import com.yapp.todakun.shared.NotificationType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class NotificationRepositoryAdapterTest(
    private val notificationJpaRepository: NotificationJpaRepository,
) : DescribeSpec(
        {
            val adapter = NotificationRepositoryAdapter(notificationJpaRepository)

            fun notification(
                memberId: UUID,
                type: NotificationType,
                title: String = "제목",
                content: String = "본문",
                deepLink: String? = null,
            ) = Notification.create(memberId, type, title, content, deepLink)

            describe("save & findAllByMemberId & countUnread") {
                it("회원 알림을 저장하고 목록·안읽음 수를 조회한다") {
                    val memberId = Uuid.generateV7().toJavaUuid()
                    adapter.save(notification(memberId, NotificationType.FORTUNE, "제목1", "본문1", "fortune/today"))
                    adapter.save(notification(memberId, NotificationType.AI_COMPLETE, "제목2", "본문2"))

                    adapter.findAllByMemberId(memberId).size shouldBe 2
                    adapter.countUnread(memberId) shouldBe 2L
                }

                it("저장 시 createdAt이 감사(auditing)로 채워진다") {
                    val memberId = Uuid.generateV7().toJavaUuid()
                    val saved = adapter.save(notification(memberId, NotificationType.NOTICE, "공지"))

                    (saved.createdAt != null) shouldBe true
                }
            }

            describe("markAsRead (단건)") {
                it("특정 알림만 읽음 처리한다") {
                    val memberId = Uuid.generateV7().toJavaUuid()
                    val saved = adapter.save(notification(memberId, NotificationType.FORTUNE))

                    adapter.save(saved.markAsRead())

                    adapter.findById(saved.id)?.isRead shouldBe true
                    adapter.countUnread(memberId) shouldBe 0L
                }
            }

            describe("markAllReadByMemberId") {
                it("해당 회원의 알림만 전부 읽음 처리한다") {
                    val memberId = Uuid.generateV7().toJavaUuid()
                    val otherId = Uuid.generateV7().toJavaUuid()
                    adapter.save(notification(memberId, NotificationType.FORTUNE))
                    adapter.save(notification(memberId, NotificationType.LUCKY_ACTION))
                    adapter.save(notification(otherId, NotificationType.FORTUNE))

                    adapter.markAllReadByMemberId(memberId)

                    adapter.countUnread(memberId) shouldBe 0L
                    adapter.countUnread(otherId) shouldBe 1L
                }
            }
        },
    )
