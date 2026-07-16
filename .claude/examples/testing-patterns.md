# Test-Pattern Templates (canonical)

The canonical per-layer `DescribeSpec` examples and the shared test infra (`TestContainersConfig` · `*Fixture` · `KotestProjectConfig`).
Rules and principles follow the `testing` skill. This document holds **only the code you copy and use**.

## Declaration/DSL-placement rule (summary)

- Constructor injection (`MockMvc`) and plain `mockk()` objects → `val`.
- `@MockkBean` is field-injected by Spring *after* construction → a **class property + `lateinit var`** is required.
- The DSL defaults to the constructor lambda `DescribeSpec({ ... })`. Use `DescribeSpec()` + `init { }` **only when `@MockkBean` is present**.

## adapter-in — JWT authentication/authorization verification

```kotlin
@WebMvcTest(UserController::class)
class UserControllerTest(
    private val mockMvc: MockMvc,                       // constructor injection → val
) : DescribeSpec() {

    @MockkBean
    private lateinit var getUserUseCase: GetUserUseCase // field injection → lateinit var

    init {
        describe("GET /users/me") {
            context("when requesting without authentication") {
                it("returns 401") {
                    mockMvc.get("/users/me")
                        .andExpect { status { isUnauthorized() } }
                }
            }

            context("when requesting with a valid JWT") {
                it("returns 200") {
                    every { getUserUseCase.getById(any()) } returns user

                    mockMvc.get("/users/me") {
                        header("Authorization", "Bearer valid-token")
                    }.andExpect { status { isOk() } }
                }
            }
        }
    }
}
```

## application — business logic, ports via MockK

```kotlin
class GetUserServiceTest : DescribeSpec({
    val userRepository = mockk<UserRepository>()
    val getUserService = GetUserService(userRepository)

    afterTest { clearMocks(userRepository) }            // prevent state leakage (mandatory)

    describe("getById") {
        context("when the ID exists") {
            it("returns the user") {
                val user = UserFixture.create()
                every { userRepository.findById(user.id) } returns user

                getUserService.getById(user.id) shouldBe user
                verify(exactly = 1) { userRepository.findById(user.id) }
            }
        }

        context("when the ID does not exist") {
            it("throws UserNotFoundException") {
                every { userRepository.findById(any()) } returns null

                shouldThrow<UserNotFoundException> {
                    getUserService.getById(UserFixture.FIXED_ID)   // tests use a fixed UUID
                }
            }
        }
    }
})
```

## adapter-out — JPA return values, TestContainer

```kotlin
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)                   // inject container via composition (not inheritance)
class UserJpaAdapterTest(
    private val userJpaRepository: UserJpaRepository,   // constructor injection → val
) : DescribeSpec({

    val adapter = UserJpaAdapter(userJpaRepository)

    describe("findById") {
        context("when a saved user exists") {
            it("can be retrieved by ID") {
                val saved = adapter.save(UserFixture.create())

                val found = adapter.findById(saved.id)

                found.shouldNotBeNull()
                found.id shouldBe saved.id
            }
        }
    }
})
```

## TestContainersConfig (shared)

`@ServiceConnection` + `@TestConfiguration` composition. Singleton + `withReuse(true)` for reuse across runs (no `stop()` call). PostgreSQL is `pgvector/pgvector:pg17` (superset-compatible with postgres) from the start, so plain JPA and VectorStore tests share it.

```kotlin
@TestConfiguration(proxyBeanMethods = false)
class TestContainersConfig {
    companion object {
        @JvmStatic
        val postgres = PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"),
        ).withInitScript("init-pgvector.sql")   // CREATE EXTENSION IF NOT EXISTS vector;
            .withReuse(true).apply { start() }

        @JvmStatic
        val redis = GenericContainer("redis:7.2").withExposedPorts(6379).withReuse(true).apply { start() }
    }

    @Bean
    @ServiceConnection
    fun postgresContainer() = postgres

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer() = redis
}
```

> Local reuse needs `testcontainers.reuse.enable=true` in `~/.testcontainers.properties`. **Reuse is disabled in CI.**

## *Fixture (per-domain fixed data)

Since PKs are UUIDv7, random values every time make assertions unstable → **fixed UUID** + default-value factory. Place it in the `fixture` package under `src/test`.

```kotlin
object UserFixture {
    val FIXED_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000001")

    fun create(
        id: UUID = FIXED_ID,
        nickname: String = "테스트유저",
        provider: OAuthProvider = OAuthProvider.GOOGLE,
    ): User = User(id = id, nickname = nickname, provider = provider)
}
```

## KotestProjectConfig (global config, one per src/test)

```kotlin
class KotestProjectConfig : AbstractProjectConfig() {
    override val isolationMode = IsolationMode.SingleInstance
    override fun extensions() = listOf(SpringExtension)
}
```
