# Kotlin Conventions Reference

The detailed reference for the `code-style` skill — general, official-Kotlin-style-guide-level rules, most of which `ktlintFormat` auto-fixes. For project-specific naming/package judgment calls, see the `code-style` skill body.

---

## Naming — Official Kotlin Rules

### Classes / Objects
- **UpperCamelCase**: `DeclarationProcessor`, `EmptyDeclarationProcessor`
- Acronyms: 2-letter ones are all caps (`IOStream`); 3+ letters capitalize only the first letter (`XmlFormatter`, `HttpInputStream`)

### Functions / Properties / Local Variables
- **lowerCamelCase**, no underscores: `processDeclarations()`, `declarationCount`

### Constants
- **SCREAMING_SNAKE_CASE**: `const val MAX_COUNT = 8`, `val USER_NAME_FIELD = "UserName"`

### Enums
- Constants are consistently either **SCREAMING_SNAKE_CASE** or **UpperCamelCase**: `RED`, `Green`

### Backing property
- Private properties take a `_` prefix:
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

## Formatting

### Indentation
- **4 spaces** (no tabs)
- Opening brace at end of line, closing brace on its own line:
```kotlin
if (condition) {
    doSomething()
} else {
    doSomethingElse()
}
```

### Horizontal whitespace
```kotlin
a + b           // space around binary operators
0..i            // no space around range operator
a++             // no space around unary operators
if (x)          // space between keyword and parenthesis
foo(1)          // no space before a function call's parenthesis
foo.bar()       // no space around . and ?.
Foo::class      // no space around ::
String?         // no space before ?
// comment      // space after //
```

### Colon
```kotlin
// Type declaration — no space before, space after
val x: Int

// Inheritance/implementation — space before
class Foo : Bar()

// Lambda return type
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

Annotations go before modifiers:
```kotlin
@Named("Foo")
private val foo: Foo
```

### File end (a recurring PR review point)

- **Every file ends with exactly one newline character (POSIX EOF).** A missing trailing newline is a common review comment on `.kt`/`.java`/`.yaml`/`.gradle.kts`/`.md` files — `ktlintFormat` fixes it for Kotlin files, but watch for this on non-Kotlin files too.

---

## Class Headers

Short: one line:
```kotlin
class Person(id: Int, name: String)
```

Long: one parameter per line + trailing comma:
```kotlin
class Person(
    id: Int,
    name: String,
    surname: String,
) : Human(id, name), KotlinMaker { /*...*/ }
```

---

## Functions

Break a long signature one parameter per line:
```kotlin
fun longMethodName(
    argument: ArgumentType = defaultValue,
    argument2: AnotherArgumentType,
): ReturnType {
    // body
}
```

Prefer the `=` form for single-expression functions:
```kotlin
fun double(x: Int) = x * 2          // good
fun double(x: Int): Int { return x * 2 }  // bad
```

---

## Properties

Simple read-only: one line:
```kotlin
val isEmpty: Boolean get() = size == 0
```

Complex getter/setter: on their own line:
```kotlin
val foo: String
    get() { /*...*/ }
```

---

## Annotations

An annotation with arguments goes on its own line:
```kotlin
@Target(AnnotationTarget.PROPERTY)
annotation class JsonExclude
```

A single argument-less annotation may share the line:
```kotlin
@Test fun foo() { /*...*/ }
```

---

## Control Flow

`else`, `catch`, `finally` go on the same line as the closing brace:
```kotlin
try {
    // body
} catch (e: Exception) {
    // catch
} finally {
    // cleanup
}
```

Multi-line `when` branches are separated by a blank line:
```kotlin
when (token) {
    is Token.Value -> callback.visit(token.value)

    Token.LBRACE -> {
        // multi-line handling
    }
}
```

Short `when` branches: no braces:
```kotlin
when (foo) {
    true -> bar()
    false -> baz()
}
```

---

## Lambdas

Space around braces and arrow:
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

## Chained Calls

Start the following line with `.` or `?.`:
```kotlin
val result = items
    .filter { it.isActive }
    .map { it.name }
    .sorted()
```

---

## Trailing Comma

Always use one in declarations (minimizes diffs, makes reordering easy):
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

## Idioms

### Prefer immutability
```kotlin
val list = listOf("a", "b")   // good
var list = arrayListOf("a")   // bad
```

### Prefer default values over overloads
```kotlin
fun foo(a: String = "a") { }   // good
fun foo() = foo("a")            // bad
fun foo(a: String) { }
```

### Prefer expression form for `if`/`when`
```kotlin
return if (x) foo() else bar()          // good
return when(x) { 0 -> "zero" else -> "nonzero" }

if (x) return foo() else return bar()   // bad
```

- 2 conditions: `if`
- 3+ conditions: `when`

### Range
```kotlin
for (i in 0..<n) { }    // good
for (i in 0..n - 1) { } // bad
```

### Strings
```kotlin
"$name has ${children.size} children"  // no braces for a simple variable
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
token?.let { authenticate(it) } ?: reject()   // good — idiomatic Kotlin
if (token != null) authenticate(token)         // discouraged — works, but not idiomatic
```

### A negated range instead of two comparisons
Fold a `>=`/`<` pair into a single range check — it reads as one intent (the complement of a day window):
```kotlin
return hour !in DAY_START until DAY_END       // good
return hour >= DAY_END || hour < DAY_START    // discouraged — two comparisons
```

### Named arguments
When a parameter is Boolean, or several parameters share the same type:
```kotlin
drawSquare(x = 10, y = 10, width = 100, height = 100, fill = true)
```

---

## Doc Comments

```kotlin
/**
 * Returns the absolute value of the given [number].
 */
fun abs(number: Int): Int { /*...*/ }
```

- Short: one line: `/** Short description. */`
- Instead of `@param` / `@return`, reference parameters directly in the body via `[paramName]` links
- Write KDoc on every public API
