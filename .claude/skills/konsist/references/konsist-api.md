# Konsist API Reference

The detailed reference for the `konsist` skill — a DSL cheat sheet for the Konsist **library itself**; open it only when writing a new rule and you need to look up an exact method name. For this project's actual rules and how to add one, see the `konsist` skill body.

---

## Scope Creation API

```kotlin
// Whole project (most commonly used)
Konsist.scopeFromProject()

// A specific module (nested modules use a slash, not a Gradle-style colon)
Konsist.scopeFromModule("auth/domain")
Konsist.scopeFromModule("user/application")

// A specific directory
Konsist.scopeFromDirectory("auth/auth-domain/src/main/kotlin")

// Production code only (excludes tests)
Konsist.scopeFromProduction()

// Test code only
Konsist.scopeFromTest()
```

---

## Declaration Selectors

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

## Filter Methods (withXxx)

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
.withPackage("..application..")     // "application" appears somewhere in the package
  .withPackage("com.yapp.todakun..") // starts at the root package
  .withPackage("..adapter.web")       // ends with "adapter.web"
  .withoutPackage("..test..")         // exclude test packages
```

### By annotation
```kotlin
.withAnnotationNamed("RestController")         // search by name
  .withAnnotationOf<RestController>()            // search by type (needs import)
  .withAllAnnotationsOf(A::class, B::class)      // has all of these annotations
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

## Declaration Properties

```kotlin
it.name                    // declaration name
it.packageName             // package name (e.g. "com.yapp.todakun.auth")
it.fullyQualifiedName      // fully qualified name

// Annotation checks
it.annotations             // list of annotations
it.hasAnnotationNamed("Entity")
it.hasAnnotationOf<Entity>()

// Import checks
it.hasImport { imp -> imp.name.contains(".adapter.") }

// Parent checks
it.hasParentInterface { iface -> iface.name.endsWith("Api") }
it.hasParentClass { cls -> cls.name == "BaseEntity" }

// Modifier checks
it.hasPublicModifier()
it.hasPrivateModifier()
it.hasAbstractModifier()
```

---

## Assertion Methods

```kotlin
// Every element must satisfy the condition
.assertTrue { it.hasPublicModifier() }

// No element may satisfy the condition
  .assertFalse { it.hasAnnotationNamed("Entity") }

// The list must be empty
  .assertEmpty()

// The list must have elements
  .assertNotEmpty()

// Extra options
  .assertTrue(
    strict = true,
    additionalMessage = "If this rule is violated, do X"
  ) { it.hasPublicModifier() }
```

---

## Architecture Layer Verification

For `assertArchitecture` syntax and how to use `Layer`/`dependsOn`/`dependsOnNothing`, this project's actual working example already lives in the `konsist` skill body under "1. This Project's Current Architecture Rules > Layer dependency direction" — not repeated here.

---

## Package Pattern Syntax

| Pattern | Meaning | Matches |
|---------|---------|---------------|
| `..domain..` | contains `domain` somewhere | `com.yapp.todakun.auth.domain.foo` |
| `com.yapp.todakun..` | starts at the root, anything after | `com.yapp.todakun.auth.application` |
| `..adapter.web` | ends with `adapter.web` | `com.yapp.todakun.auth.adapter.web` |
| `..application` | ends with `application` | `com.yapp.todakun.user.application` |
| `com.yapp.todakun.auth` | exact match | only `com.yapp.todakun.auth` |
