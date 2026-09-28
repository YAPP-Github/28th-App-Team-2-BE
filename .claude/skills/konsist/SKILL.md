---
name: konsist
description: Load when adding/modifying architecture rules (Konsist tests) or verifying layer boundaries. This project's current rules and how to add a new one. The Konsist library API itself lives in references/konsist-api.md.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Konsist Architecture Verification Rules

Run the tests: `./gradlew :architecture-test:test`
Test location: `architecture-test/src/test/kotlin/com/yapp/todakun/architecture/ArchitectureTest.kt`

> The Konsist DSL's Scope creation / declaration selectors / filter methods / assertion methods, etc. — the library's API reference — live in `references/konsist-api.md`. Open it only when writing a new rule and you need to look up an exact method name.

---

## 1. This Project's Current Architecture Rules

### Layer-protection rules

| Rule | Description |
|------|-------------|
| No Spring annotations on domain classes | A class outside `.application`, `.adapter`, `.shared`, `.common`, `com.yapp.todakun.web` (common-web) may not carry an `org.springframework.*` annotation |
| No JPA annotations on domain classes | The same target may not carry a `jakarta.persistence.*` annotation |
| CQRS service location | `@CommandService`/`@QueryService` classes exist only in the `.application` package |
| Controller → Api implementation enforced | `*Controller` classes must implement a `*Api` interface |
| UseCase location | `*UseCase` interfaces (inbound ports) exist only in `.port.inbound` (not `{domain}-domain`/`{domain}-application`) |
| Outbound Port location | `*Port` interfaces exist only in `.port.outbound` (`{domain}-domain`); excludes `shared`'s cross-domain ports (e.g. `UserAuthPort`) |
| JpaEntity location | `*JpaEntity` classes exist only in the `.adapter` package |
| Adapter location | `*Adapter` classes exist only in the `.adapter` package |
| Api interface location | `*Api` interfaces exist only in the `.adapter` package |
| RestController location | `@RestController` classes exist only in the `.adapter` package |
| Request DTO location | `*Request` classes exist only in the `.adapter` package |
| Response DTO location | `*Response` classes exist only in the `.adapter` package (`CommonResponse` is the common-web exception — `withoutName("CommonResponse")`) |
| No application → adapter references | `*-application` classes may not import the `.adapter.` package |

> The `domainClasses` filter excludes common-web (`com.yapp.todakun.web`) (`!packageName.startsWith("com.yapp.todakun.web")`). common-web uses Spring web annotations, so it isn't subject to the domain rules.

### Layer dependency direction (assertArchitecture)

```kotlin
Konsist.scopeFromProject().assertArchitecture {
  val domain     = Layer("Domain",      "com.yapp.todakun.auth..")   // example
  val application = Layer("Application", "com.yapp.todakun..application..")
  val adapter    = Layer("Adapter",     "com.yapp.todakun..adapter..")

  domain.dependsOnNothing()
  application.dependsOn(domain)
  adapter.dependsOn(application, domain)
}
```

---

## 2. How to Add a New Rule

Add a `@Test` method to `ArchitectureTest.kt`.

```kotlin
@Test
fun `new rule name`() {
  scope
    .classes()
    .withNameEndingWith("Service")          // 1. select the target
    .assertTrue {                           // 2. verify the condition
      it.packageName.contains(".application")
    }
}
```

### Things to consider when adding a rule

- Write the test name in Korean so the rule's intent is clear
- Reuse `scope` from `Konsist.scopeFromProject()` (`private val`)
- Use the current `domainClasses` computed property for the domain-class filter:
  ```kotlin
  private val domainClasses
      get() = scope.classes().filter { clazz ->
          !clazz.packageName.contains(".application") &&
          !clazz.packageName.contains(".adapter") &&
          !clazz.packageName.contains(".shared") &&
          !clazz.packageName.contains(".common") &&
          !clazz.packageName.contains(".architecture")
      }
  ```
- When adding a new domain, also add its `testImplementation` to `architecture-test/build.gradle.kts`
