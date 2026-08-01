package com.yapp.todakun.auth.adapter.persistence

import com.yapp.todakun.auth.config.JpaAuditingTestConfig
import com.yapp.todakun.auth.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PROVIDER_ID = "apple-provider-id"
private const val CLIENT_ID = "com.yapp.todakun"
private const val REFRESH_TOKEN = "apple-refresh-token"
private const val UPDATED_REFRESH_TOKEN = "apple-refresh-token-updated"

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class, JpaAuditingTestConfig::class)
class AppleOauthCredentialAdapterTest(
    private val appleOauthCredentialJpaRepository: AppleOauthCredentialJpaRepository,
) : DescribeSpec(
        {
            val adapter = AppleOauthCredentialAdapter(appleOauthCredentialJpaRepository)

            describe("save") {
                context("신규 providerId로 저장하면") {
                    it("저장된 credential을 반환한다") {
                        val saved = adapter.save(PROVIDER_ID, CLIENT_ID, REFRESH_TOKEN)

                        saved.providerId shouldBe PROVIDER_ID
                        saved.clientId shouldBe CLIENT_ID
                        saved.refreshToken shouldBe REFRESH_TOKEN
                    }
                }

                context("이미 존재하는 providerId로 다시 저장하면") {
                    it("같은 행의 clientId와 refreshToken을 갱신해 반환한다(upsert)") {
                        val original = adapter.save(PROVIDER_ID, CLIENT_ID, REFRESH_TOKEN)
                        val updatedRefreshToken = UPDATED_REFRESH_TOKEN

                        val updated = adapter.save(PROVIDER_ID, CLIENT_ID, updatedRefreshToken)

                        updated.id shouldBe original.id
                        updated.refreshToken shouldBe updatedRefreshToken
                    }
                }
            }

            describe("find") {
                context("존재하지 않는 providerId로 조회하면") {
                    it("null을 반환한다") {
                        val nonExistentProviderId = Uuid.generateV7().toString()

                        adapter.find(nonExistentProviderId).shouldBeNull()
                    }
                }
            }

            describe("delete") {
                context("저장된 providerId를 삭제하면") {
                    it("더 이상 조회되지 않는다") {
                        adapter.save(PROVIDER_ID, CLIENT_ID, REFRESH_TOKEN)

                        adapter.delete(PROVIDER_ID)

                        adapter.find(PROVIDER_ID).shouldBeNull()
                    }
                }
            }
        },
    )
