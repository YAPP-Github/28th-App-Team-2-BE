package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.config.TestContainersConfig
import com.yapp.todakun.member.fixture.MemberFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class MemberRepositoryAdapterTest(
    private val memberJpaRepository: MemberJpaRepository,
) : DescribeSpec({
        val adapter = MemberRepositoryAdapter(memberJpaRepository)

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
                    val member = MemberFixture.create()
                    adapter.save(member)
                    val updatedName = "전우치"

                    val updated = adapter.save(member.copy(name = updatedName))

                    updated.name shouldBe updatedName
                    adapter.findById(member.id)?.name shouldBe updatedName
                }
            }
        }

        describe("findById") {
            context("저장된 ID로 조회하면") {
                it("해당 회원을 반환한다") {
                    val member = MemberFixture.create()
                    adapter.save(member)

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
    }
)
