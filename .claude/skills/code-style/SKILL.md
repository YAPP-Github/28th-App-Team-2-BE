---
name: code-style
description: Load when writing or modifying Kotlin code. Naming conventions, package structure, formatting, modifier order, idioms, ktlint rules.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Code Style Rules

Passing Ktlint is required: `./gradlew ktlintCheck` / auto-fix: `./gradlew ktlintFormat`

---

## 1. Naming — Project-Specific

| Type | Rule | Example |
|------|------|---------|
| Domain entity (Kotlin) | Domain noun | `User` |
| JPA entity (Java) | `*JpaEntity` | `UserJpaEntity` |
| Inbound port | `*UseCase` | `GetUserUseCase` |
| Outbound port | `*Repository` / `*Port` | `UserRepository` |
| Adapter implementation | `*Adapter` | `UserJpaAdapter` |
| Swagger interface | `*Api` | `UserApi` |
| Controller | `*Controller` | `UserController` |
| Request DTO | `*Request` | `UpdateUserRequest` |
| Response DTO | `*Response` | `UserResponse` |
| Response-code enum | `*ErrorCode` / `*SuccessCode` (implements `ResponseCode`) | `UserErrorCode` |
| Command service | `@CommandService` + `*Service` | `CreateUserService` |
| Query service | `@QueryService` + `*Service` | `GetUserService` |

### Naming consistency (recurring PR-review points)

These come up in review over and over — apply them up front.

- **Put the target entity noun in the name.** A use case that withdraws a *member* is `WithdrawMemberUseCase`, not a bare `WithdrawUseCase` — match the existing sibling (`UpdateMemberUseCase`). The same noun flows through the whole vertical slice.
- **Rename the whole family together, in the same commit.** When one name changes, rename its siblings *and its test*: `*UseCase` · `*Command` · `*Request` · `*Service` · `*ServiceTest`. A half-renamed slice (`WithdrawMemberService` left with `WithdrawRequest`) is exactly what reviewers flag.
- **`shared` port names use a concrete resource/entity noun, never a vague one.** `DeleteMemberSajusPort` (concrete `Sajus`), not `DeleteMemberSajuDataPort` (vague `Data`) — mirror the sibling ports (`CreateSajuChartPort`, `RevokeMemberTokensPort`, `CreateMemberPort`).
- **Use the domain's real id name.** A member id is `memberId`, not a generic `userId`, even inside a `shared` port signature — stay consistent with the rest of the codebase.
- **Pick the verb prefix by semantics.** `Create*` = pure new creation (`CreateMemberUseCase`). `Save*` = upsert / create-or-update (`SaveTermsAgreementUseCase`: first submit inserts, re-submit updates). Don't default to `Create` when the operation also updates.

---

## 2. Packages & Modules

**Root-level modules**
```
com.yapp.todakun.common            # AppException, ResponseCode, @CommandService/@QueryService
com.yapp.todakun.web               # common-web: CommonResponse, GlobalExceptionHandler, @DisableSwaggerSecurity
com.yapp.todakun.shared            # UserId, UserAuthPort
com.yapp.todakun.bootstrap         # AppApplication (entry point)
```

**Domain modules** (nested as `{domain}/{domain}-*`)
```
com.yapp.todakun.{domain}                    # {domain}-domain
com.yapp.todakun.{domain}.application        # {domain}-application
com.yapp.todakun.{domain}.adapter.web        # {domain}-adapter-in
com.yapp.todakun.{domain}.adapter.{tech}     # {domain}-adapter-out
```

Example (auth domain):
```
com.yapp.todakun.auth                  # auth-domain
com.yapp.todakun.auth.application      # auth-application
com.yapp.todakun.auth.adapter.web      # auth-adapter-in
com.yapp.todakun.auth.adapter.jwt      # auth-adapter-out (JWT)
com.yapp.todakun.auth.adapter.oauth    # auth-adapter-out (OAuth)
com.yapp.todakun.auth.adapter.redis    # auth-adapter-out (Redis)
```

- Package names are always **lowercase**, no underscores
- When adding a new domain, register all 4 modules together in `settings.gradle.kts`

### File organization — one public type per file (recurring PR-review points)

Reviewers consistently ask for these splits; do them from the start rather than bundling.

- **One `*Request`/`*Response` DTO per file**, file name == class name. Don't pack `RegisterDeviceTokenRequest` + `UnregisterDeviceTokenRequest` into one `DeviceTokenRequests.kt`.
- **One `*UseCase` per file.** Don't merge `GetNotificationSettingUseCase` + `UpdateNotificationSettingUseCase` into a `NotificationSettingUseCases.kt`.
- **Never mix a Query port and a Command port in one file.** A query port (`GetNotificationsUseCase`) and a command port (`ReadNotificationUseCase`) go in separate files — the `@QueryService`/`@CommandService` (CQRS) boundary must be visible in the file layout, not hidden inside a shared `*UseCases.kt`.
- **One exception per file.** Split domain exceptions one-per-file (`NotificationNotFoundException.kt`, `NotificationAccessDeniedException.kt`, `PushSendFailedException.kt`) — don't collect them in a single `*Exceptions.kt`. Match the existing `member`/`saju`/`terms` layout.

---

## 3. Naming — Official Kotlin Rules

### Classes / objects
- **UpperCamelCase**: `DeclarationProcessor`, `EmptyDeclarationProcessor`
- Acronyms: 2-letter ones fully uppercase (`IOStream`); 3+ letters capitalize only the first (`XmlFormatter`, `HttpInputStream`)

### Functions / properties / local variables
- **lowerCamelCase**, no underscores: `processDeclarations()`, `declarationCount`

### Constants
- **SCREAMING_SNAKE_CASE**: `const val MAX_COUNT = 8`, `val USER_NAME_FIELD = "UserName"`

### Enums
- Constants are consistently either **SCREAMING_SNAKE_CASE** or **UpperCamelCase**: `RED`, `Green`

### Backing properties
- Use a `_` prefix for the private property:
```kotlin
private val _items = mutableListOf<Item>()
val items: List<Item> get() = _items
```

### Test methods
- Backticks + spaces allowed: `` fun `ensure everything works`() ``

### Class names
- Nouns/noun phrases: `List`, `PersonReader`
- Avoid meaningless words like `Manager`, `Wrapper`, `Util`

---

## 4. Formatting

### Indentation
- **4 spaces** (no tabs)
- Opening brace at line end, closing brace on its own line:
```kotlin
if (condition) {
    doSomething()
} else {
    doSomethingElse()
}
```

### Horizontal whitespace
```kotlin
a + b           // spaces around binary operators
0..i            // no spaces around range operator
a++             // no spaces around unary operators
if (x)          // space between keyword and parenthesis
foo(1)          // no space before function-call parenthesis
foo.bar()       // no spaces around . and ?.
Foo::class      // no spaces around ::
String?         // no space before ?
// comment      // space after //
```

### Colon
```kotlin
// type declaration — no space before, space after
val x: Int

// inheritance/implementation — space before
class Foo : Bar()

// lambda return type
val f: (Int) -> String
```

### Modifier order
```
public/protected/private/internal
expect/actual
final/open/abstract/sealed/const
external
override
lateinit
tailrec
vararg
suspend
inner
enum/annotation/fun
companion
inline/value
infix
operator
data
```

Write annotations before modifiers:
```kotlin
@Named("Foo")
private val foo: Foo
```

### End of file (recurring PR-review point)

- **Every file ends with exactly one trailing newline (POSIX EOF).** A missing final newline is a frequent review comment across `.kt`/`.java`/`.yaml`/`.gradle.kts`/`.md` files — `ktlintFormat` fixes Kotlin, but keep it in mind for non-Kotlin files too.

---

## 5. Class Headers

If short, one line:
```kotlin
class Person(id: Int, name: String)
```

If long, one parameter per line + trailing comma:
```kotlin
class Person(
    id: Int,
    name: String,
    surname: String,
) : Human(id, name), KotlinMaker { /*...*/ }
```

---

## 6. Functions

Break long signatures onto one parameter per line:
```kotlin
fun longMethodName(
    argument: ArgumentType = defaultValue,
    argument2: AnotherArgumentType,
): ReturnType {
    // body
}
```

Prefer the `=` form for single expressions:
```kotlin
fun double(x: Int) = x * 2          // good
fun double(x: Int): Int { return x * 2 }  // bad
```

---

## 7. Properties

Simple read-only on one line:
```kotlin
val isEmpty: Boolean get() = size == 0
```

Complex getter/setter on separate lines:
```kotlin
val foo: String
    get() { /*...*/ }
```

---

## 8. Annotations

Annotations with arguments on a separate line:
```kotlin
@Target(AnnotationTarget.PROPERTY)
annotation class JsonExclude
```

A single annotation without arguments may share the line:
```kotlin
@Test fun foo() { /*...*/ }
```

---

## 9. Control Flow

`else`, `catch`, `finally` on the same line as the closing brace:
```kotlin
try {
    // body
} catch (e: Exception) {
    // catch
} finally {
    // cleanup
}
```

Separate multi-line `when` branches with blank lines:
```kotlin
when (token) {
    is Token.Value -> callback.visit(token.value)

    Token.LBRACE -> {
        // multi-line handling
    }
}
```

Short `when` branches without braces:
```kotlin
when (foo) {
    true -> bar()
    false -> baz()
}
```

---

## 10. Lambdas

Spaces around braces and the arrow:
```kotlin
list.filter { it > 10 }
appendCommaSeparated(properties) { prop ->
    val value = prop.get(obj)
    // ...
}
```

When a single lambda is the only argument, move it outside the parentheses:
```kotlin
run { println("hello") }
```

---

## 11. Chained Calls

Start the next line with `.` or `?.`:
```kotlin
val result = items
    .filter { it.isActive }
    .map { it.name }
    .sorted()
```

---

## 12. Trailing Comma

Always use in declarations (minimizes diffs, eases reordering):
```kotlin
fun foo(
    x: Int,
    y: Int,   // trailing comma
) { }

listOf(
    "a",
    "b",   // trailing comma
)
```

---

## 13. Idioms

### Prefer immutability
```kotlin
val list = listOf("a", "b")   // good
var list = arrayListOf("a")   // bad
```

### Replace overloads with default values
```kotlin
fun foo(a: String = "a") { }   // good
fun foo() = foo("a")            // bad
fun foo(a: String) { }
```

### Prefer the expression form of `if` / `when`
```kotlin
return if (x) foo() else bar()          // good
return when(x) { 0 -> "zero" else -> "nonzero" }

if (x) return foo() else return bar()   // bad
```

- 2 conditions: `if`
- 3+ conditions: `when`

### Ranges
```kotlin
for (i in 0..<n) { }    // good
for (i in 0..n - 1) { } // bad
```

### Strings
```kotlin
"$name has ${children.size} children"  // no braces for simple variables
println("""
    multiline
""".trimIndent())
```

### Nullable Boolean
```kotlin
if (value == true) { }    // good
if (value != false) { }   // bad
```

### Null handling — prefer `?.let` + elvis over `!= null`
```kotlin
token?.let { authenticate(it) } ?: reject()   // good — Kotlin idiom
if (token != null) authenticate(token)         // avoid — works, but not idiomatic
```

### Negated range instead of two comparisons
Fold a `>=`/`<` pair into a single range check — it reads as one intent (the complement of the day window):
```kotlin
return hour !in DAY_START until DAY_END       // good
return hour >= DAY_END || hour < DAY_START    // avoid — two comparisons
```

### Named arguments
For Boolean parameters or when several params share a type:
```kotlin
drawSquare(x = 10, y = 10, width = 100, height = 100, fill = true)
```

---

## 14. Doc Comments

```kotlin
/**
 * Returns the absolute value of the given [number].
 */
fun abs(number: Int): Int { /*...*/ }
```

- One line if short: `/** Short description. */`
- Instead of `@param` / `@return`, reference parameters directly in the body via `[paramName]` links
- Write KDoc for all public API

---

## 15. Environment Variables & Gradle

- Manage secrets via `.env`, commit only `.env.example`
- Use `build.gradle.kts` (Kotlin DSL). Groovy DSL is forbidden.
