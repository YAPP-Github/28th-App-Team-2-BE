package com.yapp.todakun.chat.adapter.persistence

import com.yapp.todakun.chat.config.JpaAuditingTestConfig
import com.yapp.todakun.chat.config.TestContainersConfig
import com.yapp.todakun.chat.fixture.ChatFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class, JpaAuditingTestConfig::class)
class ChatConversationRepositoryAdapterTest(
    private val chatConversationJpaRepository: ChatConversationJpaRepository,
) : DescribeSpec(
        {
            val adapter = ChatConversationRepositoryAdapter(chatConversationJpaRepository)

            describe("save") {
                context("대화를 저장하면") {
                    it("저장된 대화를 반환하고 createdAt이 감사(auditing)로 채워진다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val conversation = ChatFixture.conversation(memberId = memberId, title = "오늘의 질문")

                        val saved = adapter.save(conversation)

                        saved.id shouldBe conversation.id
                        saved.memberId shouldBe memberId
                        saved.title shouldBe "오늘의 질문"
                        saved.unread shouldBe conversation.unread
                        saved.createdAt.shouldNotBeNull()
                    }
                }
            }

            describe("findById") {
                context("저장된 id로 조회하면") {
                    it("해당 대화를 반환한다") {
                        val saved = adapter.save(ChatFixture.conversation())

                        val found = adapter.findById(saved.id)

                        found.shouldNotBeNull()
                        found.id shouldBe saved.id
                    }
                }

                context("존재하지 않는 id로 조회하면") {
                    it("null을 반환한다") {
                        val nonExistentId = Uuid.generateV7().toJavaUuid()

                        adapter.findById(nonExistentId).shouldBeNull()
                    }
                }
            }

            describe("findAllByMemberIdOrderByLastMessageAtDesc") {
                context("한 회원의 대화가 여러 건이면") {
                    it("최근 활동 순으로 정렬해 해당 회원의 대화만 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val otherMemberId = Uuid.generateV7().toJavaUuid()
                        val older =
                            adapter.save(
                                ChatFixture.conversation(
                                    id = Uuid.generateV7().toJavaUuid(),
                                    memberId = memberId,
                                    lastMessageAt = Instant.parse("2026-08-14T00:00:00Z"),
                                ),
                            )
                        val newer =
                            adapter.save(
                                ChatFixture.conversation(
                                    id = Uuid.generateV7().toJavaUuid(),
                                    memberId = memberId,
                                    lastMessageAt = Instant.parse("2026-08-15T00:00:00Z"),
                                ),
                            )
                        adapter.save(ChatFixture.conversation(id = Uuid.generateV7().toJavaUuid(), memberId = otherMemberId))

                        val result = adapter.findAllByMemberIdOrderByLastMessageAtDesc(memberId)

                        result.map { it.id } shouldBe listOf(newer.id, older.id)
                    }
                }
            }

            describe("deleteById") {
                context("저장된 대화를 삭제하면") {
                    it("이후 조회 시 null을 반환한다") {
                        val saved = adapter.save(ChatFixture.conversation(id = Uuid.generateV7().toJavaUuid()))

                        adapter.deleteById(saved.id)

                        adapter.findById(saved.id).shouldBeNull()
                    }
                }
            }
        },
    )
