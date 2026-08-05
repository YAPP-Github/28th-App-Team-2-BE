package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.config.TestContainersConfig
import com.yapp.todakun.member.exception.MemberAlreadyExistsException
import com.yapp.todakun.member.fixture.MemberFixture
import com.yapp.todakun.shared.OauthProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
class MemberRepositoryAdapterTest(
    private val memberJpaRepository: MemberJpaRepository,
) : DescribeSpec(
        {
            val adapter = MemberRepositoryAdapter(memberJpaRepository)

            fun savedMember(
                id: UUID = Uuid.generateV7().toJavaUuid(),
                providerId: String = Uuid.generateV7().toString(),
            ) = adapter.save(MemberFixture.create(id = id, providerId = providerId))

            describe("save") {
                context("신규 회원을 저장하면") {
                    it("저장된 회원을 반환한다") {
                        val member = MemberFixture.create()

                        val saved = adapter.save(member)

                        saved shouldBe member
                    }
                }

                context("이미 존재하는 ID로 다시 저장하면") {
                    it("기존 회원 정보를 갱신한다") {
                        val member = savedMember()
                        val updatedName = "전우치"

                        val updated = adapter.save(member.copy(name = updatedName))

                        updated.name shouldBe updatedName
                        adapter.findById(member.id)?.name shouldBe updatedName
                    }
                }

                context("동일한 oauthProvider와 providerId를 가진 다른 회원을 저장하면") {
                    it("MemberAlreadyExistsException을 던진다") {
                        val member = savedMember()
                        val duplicatedMember = member.copy(id = Uuid.generateV7().toJavaUuid())

                        shouldThrow<MemberAlreadyExistsException> { adapter.save(duplicatedMember) }
                    }
                }
            }

            describe("findById") {
                context("저장된 ID로 조회하면") {
                    it("해당 회원을 반환한다") {
                        val member = savedMember()

                        val found = adapter.findById(member.id)

                        found.shouldNotBeNull()
                        found shouldBe member
                    }
                }

                context("존재하지 않는 ID로 조회하면") {
                    it("null을 반환한다") {
                        val nonExistentId = Uuid.generateV7().toJavaUuid()

                        val found = adapter.findById(nonExistentId)

                        found.shouldBeNull()
                    }
                }
            }

            describe("findIdByOauth") {
                context("저장된 OAuth 제공자와 providerId로 조회하면") {
                    it("해당 회원의 id를 반환한다") {
                        val member = savedMember()

                        val found = adapter.findIdByOauth(member.oauthProvider, member.providerId)

                        found shouldBe member.id
                    }
                }

                context("일치하는 OAuth 정보가 없으면") {
                    it("null을 반환한다") {
                        val nonExistentProviderId = Uuid.generateV7().toString()

                        val found = adapter.findIdByOauth(OauthProvider.KAKAO, nonExistentProviderId)

                        found.shouldBeNull()
                    }
                }
            }

            describe("findIdsAfter") {
                context("커서가 null이면") {
                    it("처음부터 id 오름차순으로 limit개까지 반환한다") {
                        val member1 = savedMember()
                        val member2 = savedMember()

                        val found = adapter.findIdsAfter(null, 10)

                        found shouldBe listOf(member1.id, member2.id).sorted()
                    }
                }

                context("커서를 지정하면") {
                    it("커서 이후의 id만 반환한다") {
                        val member1 = savedMember()
                        val member2 = savedMember()
                        val sortedIds = listOf(member1.id, member2.id).sorted()

                        val found = adapter.findIdsAfter(sortedIds[0], 10)

                        found shouldBe listOf(sortedIds[1])
                    }
                }

                context("limit보다 대상 회원이 많으면") {
                    it("limit 개수만큼만 반환한다") {
                        val member1 = savedMember()
                        val member2 = savedMember()
                        val member3 = savedMember()

                        val found = adapter.findIdsAfter(null, 2)

                        found shouldBe listOf(member1.id, member2.id, member3.id).sorted().take(2)
                    }
                }
            }

            describe("deleteById") {
                context("저장된 회원을 삭제하면") {
                    it("더 이상 조회되지 않는다") {
                        val member = savedMember()

                        adapter.deleteById(member.id)

                        adapter.findById(member.id).shouldBeNull()
                    }
                }
            }
        },
    )
