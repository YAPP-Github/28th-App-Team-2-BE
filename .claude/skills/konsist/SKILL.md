---
name: konsist
description: Load when adding/modifying architecture rules (Konsist tests) or verifying layer boundaries. Konsist API reference, project rules, how to add rules.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Konsist Architecture-Verification Rules

Run tests: `./gradlew :architecture-test:test`
Test location: `architecture-test/src/test/kotlin/com/yapp/todakun/architecture/ArchitectureTest.kt`

---

## 1. Current Project Architecture Rules

### Layer-protection rules

| Rule | Description |
|------|-------------|
| No Spring annotations on domain classes | Classes outside `.application`, `.adapter`, `.shared`, `.common`, `com.yapp.todakun.web` (common-web) may not have `org.springframework.*` annotations |
| No JPA annotations on domain classes | The same targets may not have `jakarta.persistence.*` annotations |
| CQRS service location | `@CommandService`/`@QueryService` classes live only in the `.application` package |
| Controller → Api implementation enforced | `*Controller` classes must implement a `*Api` interface |
| UseCase location | `*UseCase` interfaces live only in the `.application` package |
| JpaEntity location | `*JpaEntity` classes live only in the `.adapter` package |
| Adapter location | `*Adapter` classes live only in the `.adapter` package |
| Api interface location | `*Api` interfaces live only in the `.adapter` package |
| RestController location | `@RestController` classes live only in the `.adapter` package |
| Request DTO location | `*Request` classes live only in the `.adapter` package |
| Response DTO location | `*Response` classes live only in the `.adapter` package (`CommonResponse` is a common-web exception — `withoutName("CommonResponse")`) |
| No application → adapter references | `*-application` classes may not import the `.adapter.` package |

> The `domainClasses` filter excludes common-web (`com.yapp.todakun.web`) (`!packageName.startsWith("com.yapp.todakun.web")`). common-web uses Spring web annotations, so it is not subject to domain rules.

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

## 2. Scope-Creation API

```kotlin
// Whole project (most commonly used)
Konsist.scopeFromProject()

// Specific module (specify nested modules by path)
Konsist.scopeFromModule("auth:domain")
Konsist.scopeFromModule("user:application")

// Specific directory
Konsist.scopeFromDirectory("auth/auth-domain/src/main/kotlin")

// Production code only (tests excluded)
Konsist.scopeFromProduction()

// Test code only
Konsist.scopeFromTest()
```

---

## 3. Declaration Selectors

```kotlin
val scope = Konsist.scopeFromProject()

scope.classes()       // all classes
scope.interfaces()    // all interfaces
scope.objects()       // all objects
scope.functions()     // all top-level functions
scope.properties()    // all top-level properties
scope.files()         // all files
scope.packages()      // all packages
scope.imports()       // all imports
scope.typeAliases()   // all type aliases
```

---

## 4. Filter Methods (withXxx)

### By name
```kotlin
.withName("Foo")                    // exactly "Foo"
  .withNameContaining("Service")      // name contains "Service"
  .withNameStartingWith("Abstract")   // starts with "Abstract"
  .withNameEndingWith("Controller")   // ends with "Controller"
  .withNameMatching(Regex("Foo.*"))   // regex match
```

### By package (`..` = any segment)
```kotlin
.withPackage("..application..")     // contains "application" somewhere in the package
  .withPackage("com.yapp.todakun..") // starts with the root package
  .withPackage("..adapter.web")       // ends with "adapter.web"
  .withoutPackage("..test..")         // excludes the test package
```

### By annotation
```kotlin
.withAnnotationNamed("RestController")         // search by name
  .withAnnotationOf<RestController>()            // search by type (import required)
  .withAllAnnotationsOf(A::class, B::class)      // has all annotations
  .withSomeAnnotationsOf(A::class, B::class)     // has at least one
```

### By parent class / interface
```kotlin
.withParentClass { it.name == "BaseEntity" }
  .withParentInterface { it.name.endsWith("Api") }
  .withParentOf<SomeClass>()
```

### By modifier
```kotlin
.withPublicModifier()
  .withPrivateModifier()
  .withAbstractModifier()
  .withDataModifier()
  .withOpenModifier()
  .withSealedModifier()
```

---

## 5. Declaration Properties

```kotlin
it.name                    // declaration name
it.packageName             // package name (e.g. "com.yapp.todakun.auth")
it.fullyQualifiedName      // fully qualified name

// annotation checks
it.annotations             // list of annotations
it.hasAnnotationNamed("Entity")
it.hasAnnotationOf<Entity>()

// import checks
it.hasImport { imp -> imp.name.contains(".adapter.") }

// parent checks
it.hasParentInterface { iface -> iface.name.endsWith("Api") }
it.hasParentClass { cls -> cls.name == "BaseEntity" }

// modifier checks
it.hasPublicModifier()
it.hasPrivateModifier()
it.hasAbstractModifier()
```

---

## 6. Assertion Methods

```kotlin
// every element must satisfy the condition
.assertTrue { it.hasPublicModifier() }

// no element may satisfy the condition
  .assertFalse { it.hasAnnotationNamed("Entity") }

// the list must be empty
  .assertEmpty()

// the list must have elements
  .assertNotEmpty()

// extra options
  .assertTrue(
    strict = true,
    additionalMessage = "If this rule is violated, do X"
  ) { it.hasPublicModifier() }
```

---

## 7. Architecture-Layer Verification

```kotlin
Konsist.scopeFromProject().assertArchitecture {
  val domain      = Layer("Domain",      "com.yapp.todakun..") // exclude .application/.adapter via filter
  val application = Layer("Application", "com.yapp.todakun..application..")
  val adapter     = Layer("Adapter",     "com.yapp.todakun..adapter..")

  domain.dependsOnNothing()                // no external dependencies
  application.dependsOn(domain)            // may depend only on domain
  adapter.dependsOn(application, domain)   // may depend on application, domain
}
```

---

## 8. Package-Pattern Syntax

| Pattern | Meaning | Example match |
|---------|---------|---------------|
| `..domain..` | contains `domain` somewhere | `com.yapp.todakun.auth.domain.foo` |
| `com.yapp.todakun..` | starts at root, anything after | `com.yapp.todakun.auth.application` |
| `..adapter.web` | ends with `adapter.web` | `com.yapp.todakun.auth.adapter.web` |
| `..application` | ends with `application` | `com.yapp.todakun.user.application` |
| `com.yapp.todakun.auth` | exact match | only `com.yapp.todakun.auth` |

---

## 9. How to Add a New Rule

Add a `@Test` method to `ArchitectureTest.kt`.

```kotlin
@Test
fun `new rule name`() {
  scope
    .classes()
    .withNameEndingWith("Service")          // 1. select targets
    .assertTrue {                           // 2. verify the condition
      it.packageName.contains(".application")
    }
}
```

### Considerations when adding rules

- Write test names in Korean, making the rule's intent clear
- Reuse `Konsist.scopeFromProject()` for `scope` (`private val`)
- For the domain-class filter, use the current `domainClasses` computed property:
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
- When adding a new domain, you also need to add a `testImplementation` for it in `architecture-test/build.gradle.kts`