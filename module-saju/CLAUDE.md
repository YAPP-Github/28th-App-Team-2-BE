# saju

Owns the calculation and storage of 사주 charts (명식), and the record of which member owns which chart.

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| Chart calculation from bundled 만세력 data; chart persistence | Fortune interpretation text → `daily-fortune`, `day-fortune`, `year-fortune` |
| Member↔chart ownership (`MemberSajuLink`, `SELF`/`PARTNER` roles) | Compatibility scoring → `compatibility` |
| Pillar lookups (일진, 연주) served to other domains | Member profile and account lifecycle → `member` |

Partner charts are this domain's own concern (register/update/delete a partner's 명식), but *what a partnership means* — relationship scoring, narrative — is not.

## Cross-Domain Contracts

On the port channel this domain is a **pure provider**: it implements eight `shared` ports and consumes none.

| Port | Consumed by | What the consumer branches or acts on |
|------|-------------|---------------------------------------|
| `CreateSajuChartPort` | `auth` | creates the member's own chart inside the signup transaction |
| `GetSajuChartPort` | `chat`, `daily-fortune`, `day-fortune`, `year-fortune` | reads the `SELF` chart summary to ground generation |
| `GetDailyPillarPort` | `daily-fortune`, `day-fortune` | takes the day's pillar to interpret |
| `GetYearPillarPort` | `year-fortune` | takes the year's pillar to interpret |
| `GetSajuChartsForCompatibilityPort`, `GetSajuChartNamePort` | `compatibility` | reads both charts and their labels to score a pair |
| `DeleteMemberSajusPort`, `ReplaceSelfSajuChartPort` | `member` | withdrawal cleanup; re-registering the member's own chart |

**All eight are implemented by services in `saju-application`, not in `saju-adapter-out`.** This differs from the root `CLAUDE.md` example (`GetMemberPort` ← `member-adapter-out`): here each cross-domain entry point *is* a use case — calculate-and-store, or a role-filtered read — rather than a row fetch. When adding a port, decide by whether use-case logic is involved; a pure row fetch still belongs in `adapter-out`.

There is a **second channel that is not a port**. `ReplaceSelfSajuChartService` and `DeleteMemberSajusService` publish `shared.event.SajuChartChangedEvent`; it is consumed by this domain's own `SajuChartCacheEvictListener` and by `year-fortune`'s `SajuChartChangedEventListener` (`@TransactionalEventListener(AFTER_COMMIT)`), both to evict caches that a changed chart invalidates.

Pick the channel by who needs to know:

| | Port | Event |
|---|------|-------|
| Publisher knows the counterpart | yes — it calls the port | no — it must not |
| Timing | synchronous, inside the caller's decision | after commit, fan-out |
| Use for | the caller branches or acts on the result | consequences the publisher should not enumerate (cache eviction, notification) |

Adding a new consumer of a chart change means **subscribing to the event**, not adding a port — otherwise saju grows a dependency on every downstream cache. The reasoning behind using an event here rather than cache `order` is recorded in `bootstrap`'s `RedisCacheConfig`.

## Decisions & Traps

- **`docs/data-model.md` §1.2 is superseded.** It pre-designed compatibility as an `is_self` flag on `saju_chart` plus a `saju_compatibility` table. What shipped instead splits ownership into `MemberSajuChartJpaEntity` carrying `SajuRole`, and compatibility became its own domain. Treat §1.1 as current and §1.2 as a historical sketch.
- **`manseryeok/lunar-index.txt` is generated, not authored.** It was back-derived from a `solarToLunar` implementation to route around a `lunarToSolar` bug in the upstream library. Never hand-edit it — regenerate it.
- **Supported years are `1900..2050`** (`SUPPORTED_YEARS`), bounded by the bundled 만세력 data rather than by product choice. Out-of-range input raises `SajuYearOutOfRangeException`; widening the range means shipping more data, not relaxing a check.

> The chart-vs-ownership separation and its invariants are documented in `MemberSajuLink`'s KDoc. Not restated here — one source per rule.
