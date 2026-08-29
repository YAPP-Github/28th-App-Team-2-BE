package com.yapp.todakun.chat.adapter.persistence

import com.yapp.todakun.chat.ChatActionType
import com.yapp.todakun.chat.ChatMessageRole
import com.yapp.todakun.chat.ChatMessageStatus
import com.yapp.todakun.chat.config.JpaAuditingTestConfig
import com.yapp.todakun.chat.config.TestContainersConfig
import com.yapp.todakun.chat.fixture.ChatFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class, JpaAuditingTestConfig::class)
class ChatMessageRepositoryAdapterTest(
    private val chatMessageJpaRepository: ChatMessageJpaRepository,
) : DescribeSpec(
        {
            val adapter = ChatMessageRepositoryAdapter(chatMessageJpaRepository)

            describe("save") {
                context("액션 카드가 없는 메시지를 저장하면") {
                    it("저장된 메시지를 반환하고 createdAt이 감사(auditing)로 채워진다") {
                        val conversationId = Uuid.generateV7().toJavaUuid()
                        val message = ChatFixture.message(conversationId = conversationId, content = "안녕하세요")

                        val saved = adapter.save(message)

                        saved.id shouldBe message.id
                        saved.conversationId shouldBe conversationId
                        saved.content shouldBe "안녕하세요"
                        saved.action.shouldBeNull()
                        saved.createdAt.shouldNotBeNull()
                    }
                }

                context("액션 카드가 있는 메시지를 저장하면") {
                    it("액션 카드까지 그대로 왕복한다") {
                        val message =
                            ChatFixture.message(
                                role = ChatMessageRole.ASSISTANT,
                                status = ChatMessageStatus.COMPLETED,
                                action = ChatFixture.action(),
                            )

                        val saved = adapter.save(message)

                        val action = saved.action
                        action.shouldNotBeNull()
                        action.type shouldBe ChatActionType.CALENDAR_ADD
                        action.label shouldBe "이사 일정 추가하기"
                        action.category shouldBe "계약・이사"
                        action.date shouldBe LocalDate.of(2026, 8, 20)
                    }
                }
            }

            describe("findById") {
                context("저장된 id로 조회하면") {
                    it("해당 메시지를 반환한다") {
                        val saved = adapter.save(ChatFixture.message())

                        val found = adapter.findById(saved.id)

                        found.shouldNotBeNull()
                        found.id shouldBe saved.id
                    }
                }

                context("존재하지 않는 id로 조회하면") {
                    it("null을 반환한다") {
                        adapter.findById(Uuid.generateV7().toJavaUuid()).shouldBeNull()
                    }
                }
            }

            describe("findAllByConversationIdOrderByCreatedAtAsc") {
                context("한 대화에 메시지가 여러 건이면") {
                    it("생성 시각 오름차순으로, 동시각이면 id(v7) 오름차순으로 반환한다") {
                        val conversationId = Uuid.generateV7().toJavaUuid()
                        val first = adapter.save(ChatFixture.message(id = Uuid.generateV7().toJavaUuid(), conversationId = conversationId))
                        val second = adapter.save(ChatFixture.message(id = Uuid.generateV7().toJavaUuid(), conversationId = conversationId))
                        adapter.save(
                            ChatFixture.message(id = Uuid.generateV7().toJavaUuid(), conversationId = Uuid.generateV7().toJavaUuid()),
                        )

                        val result = adapter.findAllByConversationIdOrderByCreatedAtAsc(conversationId)

                        result.map { it.id } shouldBe listOf(first.id, second.id)
                    }
                }

                context("메시지가 없는 대화면") {
                    it("빈 목록을 반환한다") {
                        adapter.findAllByConversationIdOrderByCreatedAtAsc(Uuid.generateV7().toJavaUuid()).shouldBeEmpty()
                    }
                }
            }

            describe("findRecentByConversationId") {
                context("limit보다 메시지가 많으면") {
                    it("최신 순으로 limit 개수만큼만 반환한다") {
                        val conversationId = Uuid.generateV7().toJavaUuid()
                        val messages =
                            (1..3).map {
                                adapter.save(ChatFixture.message(id = Uuid.generateV7().toJavaUuid(), conversationId = conversationId))
                            }

                        val result = adapter.findRecentByConversationId(conversationId, limit = 2)

                        result.map { it.id } shouldBe listOf(messages[2].id, messages[1].id)
                    }
                }
            }

            describe("deleteAllByConversationId") {
                context("대화에 속한 메시지를 삭제하면") {
                    it("해당 대화의 메시지만 모두 삭제된다") {
                        val conversationId = Uuid.generateV7().toJavaUuid()
                        val otherConversationId = Uuid.generateV7().toJavaUuid()
                        adapter.save(ChatFixture.message(id = Uuid.generateV7().toJavaUuid(), conversationId = conversationId))
                        adapter.save(ChatFixture.message(id = Uuid.generateV7().toJavaUuid(), conversationId = conversationId))
                        val untouched =
                            adapter.save(ChatFixture.message(id = Uuid.generateV7().toJavaUuid(), conversationId = otherConversationId))

                        adapter.deleteAllByConversationId(conversationId)

                        adapter.findAllByConversationIdOrderByCreatedAtAsc(conversationId).shouldBeEmpty()
                        adapter.findById(untouched.id).shouldNotBeNull()
                    }
                }
            }
        },
    )
