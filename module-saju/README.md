# saju (Four Pillars) module

Computes and stores a **Saju chart** (사주 명식 — the Four Pillars: year/month/day/hour) from a birth date/time, plus derived data (Ohaeng distribution, Sipseong distribution, Sibiunseong). Results are persisted for reuse.

Domain terms are romanized to match the code enums (`Sipseong`, `HeavenlyStem`, …); hanja is kept where it aids precision.

> **The code is the source of truth.** Value objects & rules live in `saju-domain`, the schema in `*JpaEntity`, the entry point in `shared.CreateSajuChartPort`.
> **DB schema & response shape**: see [`docs/data-model.md`](docs/data-model.md).

## At a glance

| Topic | Decision |
|---|---|
| **Invocation** | Not exposed over REST. The only entry point is the cross-domain call `shared.CreateSajuChartPort.create(...)` (e.g. from auth signup). |
| **Modules** | Hexagonal, **3 modules** (`domain`/`application`/`adapter-out`), no `adapter-in`. |
| **Manseryeok engine** | `ManseryeokPort` (outbound) → `ManseryeokAdapter`. Ports manseryeok-js (KASI) data; year/month/day/hour verified against the library across the full range (1900–2050). |
| **Birth-time input** | `BirthTime` enum — twelve 2-hour traditional Sijin slots. **No true-solar-time (longitude) minute correction** (no `birth_longitude`/`corrected_*` columns). |
| **Lunar calendar** | Supported. `LunarSolarConverter` converts lunar → solar. |
| **Sipseong / Sibiunseong** | Out of library scope → implemented in `saju-domain`. |
| **Compatibility (합충형파해)** | **Follow-up** (schema pre-designed, not implemented). |
| **Stored tables** | `saju_chart` · `saju_pillar` · `saju_ohaeng` · `saju_sipseong`. |
| **DB** | PostgreSQL + JPA. Enums persisted as `@Enumerated(STRING)` → VARCHAR. `ddl-auto=validate`. |

**Core principles**
- Solar/lunar conversion, the 60-Ganji, and the 24 solar terms are owned by the ported manseryeok engine and are **not** re-stored in our DB.
- Sipseong/Sibiunseong are relative to the day master (day stem), so they fall outside the library and are computed in the domain.
- Display values (hanja, reading, label) are assembled from enums and library data — no separate code-master tables.

---

## 1. Base library (manseryeok-js)

Ported from [`urstory/manseryeok-js`](https://github.com/urstory/manseryeok-js) (`@fullstackfamily/manseryeok`, MIT).

**Provides**: range 1900–2050 based on KASI (Korea Astronomy & Space Science Institute) data; bit-packed into code (11.4 MB → 225 KB), no DB needed. Functions: `solarToLunar`/`lunarToSolar` (incl. leap months), `getGapja` (60-Ganji), `calculateSaju` (4 pillars), solar-term lookups, `SIXTY_PILLARS`/`getPillarById` (per-Ganji hangul/hanja/element/yin-yang).

**Does NOT cover → implemented in-house**

| Item | Handling |
|---|---|
| Sipseong (十星) | Day-master-relative logic (`Sipseong.of`) |
| Sibiunseong (十二運星, 포태법) | Branch-based logic (`Sibiunseong.of`) |
| Ohaeng / Sipseong distribution | Aggregated from the 4 pillars (`SajuCalculator`) |
| Compatibility (합충형파해) | In-house (follow-up) |

> **True solar time / historical standard time**: input is in 2-hour Sijin slots, so minute-level longitude correction is meaningless → **not adopted**. Pre-1912 historical standard-time changes (e.g. GMT+8:30 used 1908–1911) are likewise not reflected (see §5).

**Tables/code made unnecessary** — `solar_term`, `code_heavenly_stem`, `code_earthly_branch`, `code_ohaeng` (replaced by library `SIXTY_PILLARS` + domain enums); `code_sipseong`, `code_sibiunseong` (replaced by Kotlin enums; only a value-checking string column remains); `Partner` (replaced by `saju_chart.is_self`); `standard_time_period` (true-solar-time not adopted).

---

## 2. Architecture

`saju` is a **cross-domain internal capability** not exposed over REST, so it uses 3 modules. Entry point is `shared.CreateSajuChartPort`; other domains call through it.

```
module-saju/
 ├─ saju-domain/       Pure Kotlin. Entities (SajuChart/SajuPillar), value objects
 │                     (HeavenlyStem/EarthlyBranch/Element/YinYang/Sipseong/Sibiunseong/BirthTime …),
 │                     derived calc (SajuCalculator), outbound ports (ManseryeokPort/SajuChartRepository),
 │                     exceptions (SajuErrorCode)
 ├─ saju-application/  CreateSajuChartService(@CommandService) implements shared.CreateSajuChartPort.
 │                     Manseryeok 4 pillars → derived calc → persist, in one transaction
 └─ saju-adapter-out/  ManseryeokAdapter (engine: LunarSolarConverter/SolarTermTable) + JPA persistence
```

**Call paths**
- **Signup**: right after creating the member, `auth` calls `CreateSajuChartPort.create(memberId=<id>, isSelf=true, ...)` to compute & store the member's own chart in the **same transaction** (propagation REQUIRED).
- **Compatibility (follow-up)**: computes the partner's chart with `isSelf=false`, reusing the same port.

> Enum values are passed to the port as **strings** so they don't cross domain boundaries (e.g. `gender="FEMALE"`, `birthTime="MISI"`). Conversion failures raise `SajuInputInvalidException` (400).

**Manseryeok engine accuracy** (full range 1900–2050, 55,151 days compared)
- **Day pillar**: JDN-based 60-Ganji. Anchor constant fixed to match the library day pillar across the whole range (offset=49).
- **Month/year pillar (Ipchun boundary)**: computed from the **actual 24 solar terms** via `SolarTermTable` (resource `manseryeok/solar-terms.txt`), not an approximation table. Matches the library on 55,139/55,151 days; the 12 mismatched days are a **library bug**, so this adapter is the more correct one.
- **Hour pillar**: day stem + Sijin via 오서둔 (Wu-Shu-Dun / "Five Rats" method).
- **Lunar**: `LunarSolarConverter` uses a table reverse-extracted from `solarToLunar` across the full range (resource `manseryeok/lunar-index.txt`, 7.5 KB); verified bidirectionally over 55,121 days (443 leap-month-inclusive samples). The library's `lunarToSolar` is buggy and unused. Invalid lunar dates → 400 `SAJU_INPUT_INVALID`.

---

## 3. Calculation pipeline

`CreateSajuChartService.create(...)` runs in one transaction (joins the signup transaction with REQUIRED propagation).

1. **Validate input**: gender / calendar-type / Sijin strings → enums. Failure → `SajuInputInvalidException` (400).
2. **Compute 4 pillars**: `ManseryeokPort.calculate(birthDate, birthTime, calendarType, isLeapMonth)` → `FourPillars` (year/month/day/hour Ganji + month-pillar solar-term name).
   - Lunar input is converted to solar inside the adapter.
   - `BirthTime.UNKNOWN` → returns 3 pillars, hour omitted (`FourPillars.hour = null`).
3. **Derived calc** (`SajuCalculator`): assign Sipseong to each pillar's stem/branch. The day stem is the **day master ("일원")**, so it has no Sipseong (null).
4. Assign **Sibiunseong** (포태법) to each pillar's branch.
5. Aggregate the **Ohaeng distribution** (always 5 rows) and **Sipseong distribution** (always 10 rows, incl. zeros).
6. **Persist**: `saju_chart` + `saju_pillar` + `saju_ohaeng` + `saju_sipseong`.

---

## 4. Domain value objects (enums)

Sipseong (10) and Sibiunseong (12) are fixed myeongri (命理) constants, so they live as domain enums with labels — no DB master. The DB stores only a value-checking string; labels/meanings are read from the enums when assembling responses.

```kotlin
enum class Sipseong(val label: String) {        // judged by day-master element/yin-yang relation (Sipseong.of)
    BIGYEON("비견"),   // same element & yin-yang as day master — self-reliance, competition
    GEOPJAE("겁재"),   // same element, opposite yin-yang — rivalry, risk of wealth loss
    SIKSIN("식신"),    // day master generates it, same yin-yang — expression, prosperity
    SANGGWAN("상관"),  // day master generates it, opposite yin-yang — talent, criticism
    PYEONJAE("편재"),  // day master controls it, same yin-yang — fluid wealth, enterprise
    JEONGJAE("정재"),  // day master controls it, opposite yin-yang — stable wealth
    PYEONGWAN("편관"), // controls the day master, same yin-yang — pressure, command (칠살)
    JEONGGWAN("정관"), // controls the day master, opposite yin-yang — honor, duty
    PYEONIN("편인"),   // generates the day master, same yin-yang — unconventional learning
    JEONGIN("정인")    // generates the day master, opposite yin-yang — orthodox learning, support
}

enum class Sibiunseong(val label: String) {     // stage of a branch's energy, per day master (Sibiunseong.of)
    JANGSAENG("장생"), MOKYOK("목욕"), GWANDAE("관대"), GEONROK("건록"),
    JEWANG("제왕"),   SOE("쇠"),      BYEONG("병"),    SA("사"),
    MYO("묘"),        JEOL("절"),     TAE("태"),       YANG("양")
    // declaration order = 장생→…→양 cycle. Yang stems go forward, yin stems backward.
}
```

Supporting value objects: `HeavenlyStem` (10 stems: hanja/reading/element/yin-yang) · `EarthlyBranch` (12 branches: primary-qi element/yin-yang) · `Element` (5 Ohaeng: generating `generates()` / controlling `controls()`) · `YinYang` · `BirthTime` (12 Sijin + UNKNOWN) · `Gender` (MALE/FEMALE) · `CalendarType` (SOLAR/LUNAR) · `PillarType` (YEAR/MONTH/DAY/HOUR).

---

## 5. Error handling & policy

### Errors (`SajuErrorCode`)

| Situation | HTTP | Exception / code |
|---|---|---|
| Birth year outside supported range (1900–2050) | 422 | `SajuYearOutOfRangeException` / `SAJU-422` |
| Invalid input (leap month / day count / enum conversion failure …) | 400 | `SajuInputInvalidException` / `SAJU-400` |
| chartId not found | 404 | `SajuChartNotFoundException` / `SAJU-404` |
| Internal calc error (missing resource, undecidable judgment …) | 500 | `SajuCalculationException` / `SAJU-500` |

### Policy decisions

**Confirmed**
- Birth-time input = `BirthTime` enum (12 Sijin, 2-hour). No minute-level true-solar-time correction. Hour branch straight from the Sijin; hour stem via 오서둔.
- Pre-1912 births: no longitude correction; historical standard-time changes not reflected. `standard_time_period` unnecessary.
- Sipseong denominator = 7 (exclude the day master; with hour: 3 stems + 4 branches).
- Service range = the library range (1900–2050) as-is. Outside → 422.

**Open (needs planner discussion)**
- `member` ↔ `saju_chart` duplication: currently `member` is unchanged and `saju_chart` holds its own input fields + `member_id` (nullable). Fully migrating birth columns into `saju_chart` is a follow-up.
- Jasi handling: currently a single `BirthTime.JASI` (23:30–01:29). Ya-jasi / Jo-jasi split is a follow-up.
- Compatibility re-generation history: overwrite vs. versioned history.
- "Time unknown" guidance copy: explaining limited compatibility/Sipseong analysis.

---

## 6. Verification (tests)

> **Test-oracle note**: across the full range (55,151 days), the **12 days that disagree with the library (Dec 31 of certain years) are a manseryeok-js bug**; the adapter's JDN-based canonical value is correct. Those 12 days are verified against the **adapter canonical value, not the library** (the other 55,139 days match the library). These Dec-31 days are not within ±1 day of any solar term, so they are not in the 495-sample regression set below.

- **Year/month/day pillar** (`SolarTermPillarTest`): exhaustive comparison of **495 samples** at ±1 day of every solar term (the most accuracy-critical days) against the library `getGapja`.
- **Lunar → solar** (`LunarSolarConverterTest`): bidirectional comparison against the `solarToLunar` reverse-extraction over the full range (55,121 days; 443 leap-month-inclusive samples).
- **Ipchun / solar-month boundaries** (`ManseryeokAdapterTest`): regression on term-based year/month transitions (e.g. birth before Ipchun → previous year's Ganji).
- **Sipseong/Sibiunseong** (`SajuCalculatorTest`): day master (10) × branch (12) combinations (in-house area — especially important).
- Reference case: `2001-05-30 MISI` → year 辛巳 · month 癸巳 (입하 Ipha) · day 癸巳 · hour 己未.
- True-solar-time correction is not adopted, so `correctedTime` comparison is out of scope.
