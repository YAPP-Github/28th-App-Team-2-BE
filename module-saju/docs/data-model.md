# saju — Data Model & Response Shape

Reference for the `saju` module: full DB schema (DDL) and the display response shape. For the design overview, see [`../README.md`](../README.md).

Domain terms are romanized to match the code enums; hanja is kept where it aids precision.

---

## 1. DB schema

> The authoritative schema is the **JPA entities (`*JpaEntity`)**; PostgreSQL + Hibernate validate it via `ddl-auto=validate`. Enums are persisted with `@Enumerated(STRING)` → **VARCHAR** (not native ENUM). The DDL below is a **conceptual model** showing value domains and relationships (dialect aside).
>
> The `heavenly_stem` / `earthly_branch` / Sipseong / Sibiunseong columns have **no master-table FK**: stems/branches are library data, Sipseong/Sibiunseong are domain enums — each is its own source of truth.

### 1.1 Calculation result (implemented)

```sql
CREATE TABLE saju_chart (                       -- Saju chart header. Self and other (compat. partner) both live here, split by is_self
  id               UUID         NOT NULL,       -- UUIDv7
  member_id        UUID         NULL,           -- creating account (member.id). NULL for guest/partner input → must allow NULL
  is_self          BOOLEAN      NOT NULL,       -- TRUE=self, FALSE=other. "my chart list" filters WHERE is_self=TRUE
  name             VARCHAR(50)  NULL,           -- target name (for display)
  gender           VARCHAR(10)  NOT NULL,       -- MALE/FEMALE. used for luck-cycle (daeun) direction
  calendar_type    VARCHAR(10)  NOT NULL,       -- SOLAR/LUNAR (input calendar)
  input_date       DATE         NOT NULL,       -- user's original birth date
  birth_time       VARCHAR(10)  NOT NULL,       -- BirthTime enum. UNKNOWN → skip hour pillar. (no true-solar-time correction)
  is_leap_month    BOOLEAN      NOT NULL,       -- leap-month flag for lunar input
  is_time_unknown  BOOLEAN      NOT NULL,       -- birth time unknown (= birth_time UNKNOWN)
  solar_term_name  VARCHAR(20)  NULL,           -- solar term applied to the month pillar (snapshot, not FK)
  day_master_stem  VARCHAR(10)  NOT NULL,       -- day stem. basis for every Sipseong judgment
  created_at       TIMESTAMP    NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_saju_chart_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE saju_pillar (                      -- per-pillar detail
  id                UUID        NOT NULL,
  chart_id          UUID        NOT NULL,
  pillar_type       VARCHAR(10) NOT NULL,       -- YEAR/MONTH/DAY/HOUR
  heavenly_stem     VARCHAR(10) NOT NULL,       -- stem code (references library data)
  earthly_branch    VARCHAR(10) NOT NULL,       -- branch code (references library data)
  cheongan_sipseong VARCHAR(15) NULL,           -- stem Sipseong. NULL for the day pillar (day master)
  jiji_sipseong     VARCHAR(15) NOT NULL,       -- branch Sipseong
  sibiunseong       VARCHAR(15) NOT NULL,       -- Sibiunseong (branch-based)
  PRIMARY KEY (id),
  CONSTRAINT uk_saju_pillar_chart_type UNIQUE (chart_id, pillar_type),  -- one row per pillar per chart (guards retry/dup writes)
  CONSTRAINT fk_saju_pillar_chart FOREIGN KEY (chart_id) REFERENCES saju_chart(id)
);

CREATE TABLE saju_ohaeng (                      -- Ohaeng (Five Elements) distribution, always 5 rows
  id           UUID         NOT NULL,
  chart_id     UUID         NOT NULL,
  element      VARCHAR(10)  NOT NULL,           -- WOOD/FIRE/EARTH/METAL/WATER
  count        SMALLINT     NOT NULL,           -- chars of this element among 8 (or 6 if time unknown)
  percentage   DECIMAL(5,2) NOT NULL,           -- % of total chars
  PRIMARY KEY (id),
  CONSTRAINT uk_saju_ohaeng_chart_element UNIQUE (chart_id, element),
  CONSTRAINT fk_saju_ohaeng_chart FOREIGN KEY (chart_id) REFERENCES saju_chart(id)
);

CREATE TABLE saju_sipseong (                    -- Sipseong distribution, always 10 rows (incl. zeros)
  id            UUID         NOT NULL,
  chart_id      UUID         NOT NULL,
  sipseong_type VARCHAR(15)  NOT NULL,
  count         SMALLINT     NOT NULL,          -- chars judged as this Sipseong (day master excluded)
  percentage    DECIMAL(5,2) NOT NULL,          -- denominator = 7 with hour (3 stems + 4 branches), else 5
  PRIMARY KEY (id),
  CONSTRAINT uk_saju_sipseong_chart_type UNIQUE (chart_id, sipseong_type),
  CONSTRAINT fk_saju_sipseong_chart FOREIGN KEY (chart_id) REFERENCES saju_chart(id)
);
```

### 1.2 Compatibility (follow-up — schema pre-designed, not implemented)

Partner data is stored as a `saju_chart` with `is_self=false` (no separate table). Relationship type lives only in `saju_compatibility.relationship_type`.

```sql
CREATE TABLE saju_compatibility (
  id                  UUID         NOT NULL,
  my_chart_id         UUID         NOT NULL,    -- my chart (is_self=true)
  partner_chart_id    UUID         NOT NULL,    -- partner chart (is_self=false)
  relationship_type   VARCHAR(20)  NOT NULL,    -- LOVER/FRIEND/FAMILY/COLLEAGUE
  compatibility_score SMALLINT     NOT NULL,    -- 0..100
  headline            VARCHAR(50)  NOT NULL,
  subheadline         VARCHAR(100) NOT NULL,
  summary             VARCHAR(200) NOT NULL,
  total_analysis      TEXT         NOT NULL,    -- AI-generated overall analysis
  analysis_basis      VARCHAR(50)  NOT NULL,    -- e.g. 'Saju Palja based'
  created_at          TIMESTAMP    NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_compat_my_chart      FOREIGN KEY (my_chart_id)      REFERENCES saju_chart(id),
  CONSTRAINT fk_compat_partner_chart FOREIGN KEY (partner_chart_id) REFERENCES saju_chart(id)
);

CREATE TABLE saju_compatibility_ohaeng (        -- combined Ohaeng distribution of the two, always 5 rows
  id               UUID         NOT NULL,
  compatibility_id UUID         NOT NULL,
  element          VARCHAR(10)  NOT NULL,
  percentage       DECIMAL(5,2) NOT NULL,       -- combined, normalized %; 5 rows sum to 100
  PRIMARY KEY (id),
  CONSTRAINT fk_compat_ohaeng FOREIGN KEY (compatibility_id) REFERENCES saju_compatibility(id)
);

CREATE TABLE saju_compatibility_ganghap (       -- list of compatibility findings. PK must be id
  id               UUID         NOT NULL,
  compatibility_id UUID         NOT NULL,
  relation_type    VARCHAR(20)  NOT NULL,       -- CHEONGAN_HAP/JIJI_HAP/CHUNG/HYEONG/PA/HAE
  pillar_a_type    VARCHAR(10)  NOT NULL,       -- involved pillar in my chart
  pillar_b_type    VARCHAR(10)  NOT NULL,       -- involved pillar in partner chart
  description      VARCHAR(100) NULL,           -- e.g. 정임합(丁壬合)
  PRIMARY KEY (id),
  CONSTRAINT fk_compat_ganghap FOREIGN KEY (compatibility_id) REFERENCES saju_compatibility(id)
);
```

---

## 2. Display response shape (reference)

> `saju` has no REST layer / Presenter (compute & store only). Below is a **reference for how a future read/display API would assemble the result for the screen**. `hanja`/`reading` are assembled from the library 60-Ganji data and domain enums (not a DB lookup); `label` is read from the enums. Label strings stay Korean because they are the actual on-screen display text.

The example is the **actual computed result** for `2001-05-30 MISI · solar · female` (day master 癸; re-verified by recomputing the rules).

```jsonc
{
  "dayMaster": { "code": "GYE", "hanja": "癸", "reading": "계" },
  "pillars": [
    { "pillarType": "YEAR",  "stem": {"code":"SIN","hanja":"辛","reading":"신","element":{"code":"METAL","hanja":"金"}},
      "branch": {"code":"SA","hanja":"巳","reading":"사","element":{"code":"FIRE","hanja":"火"}},
      "cheonganSipseong": {"code":"PYEONIN","label":"편인"},
      "jijiSipseong": {"code":"JEONGJAE","label":"정재"}, "sibiunseong": {"code":"TAE","label":"태"} },
    { "pillarType": "MONTH", "stem": {"code":"GYE","hanja":"癸","reading":"계","element":{"code":"WATER","hanja":"水"}},
      "branch": {"code":"SA","hanja":"巳","reading":"사","element":{"code":"FIRE","hanja":"火"}},
      "cheonganSipseong": {"code":"BIGYEON","label":"비견"},
      "jijiSipseong": {"code":"JEONGJAE","label":"정재"}, "sibiunseong": {"code":"TAE","label":"태"} },
    { "pillarType": "DAY",   "stem": {"code":"GYE","hanja":"癸","reading":"계","element":{"code":"WATER","hanja":"水"}},
      "branch": {"code":"SA","hanja":"巳","reading":"사","element":{"code":"FIRE","hanja":"火"}},
      "cheonganSipseong": null, "cheonganDisplayOverride": "일원",
      "jijiSipseong": {"code":"JEONGJAE","label":"정재"}, "sibiunseong": {"code":"TAE","label":"태"} },
    { "pillarType": "HOUR",  "stem": {"code":"GI","hanja":"己","reading":"기","element":{"code":"EARTH","hanja":"土"}},
      "branch": {"code":"MI","hanja":"未","reading":"미","element":{"code":"EARTH","hanja":"土"}},
      "cheonganSipseong": {"code":"PYEONGWAN","label":"편관"},
      "jijiSipseong": {"code":"PYEONGWAN","label":"편관"}, "sibiunseong": {"code":"MYO","label":"묘"} }
  ],
  "ohaeng": [
    {"element":{"code":"WOOD","label":"목","hanja":"木"}, "count":0, "percentage":0.0},
    {"element":{"code":"FIRE","label":"화","hanja":"火"}, "count":3, "percentage":37.5},
    {"element":{"code":"EARTH","label":"토","hanja":"土"}, "count":2, "percentage":25.0},
    {"element":{"code":"METAL","label":"금","hanja":"金"}, "count":1, "percentage":12.5},
    {"element":{"code":"WATER","label":"수","hanja":"水"}, "count":2, "percentage":25.0}
  ],
  "sipseong": [
    {"type":{"code":"BIGYEON","label":"비견"}, "count":1, "percentage":14.29},
    {"type":{"code":"JEONGJAE","label":"정재"}, "count":3, "percentage":42.86},
    {"type":{"code":"PYEONGWAN","label":"편관"}, "count":2, "percentage":28.57},
    {"type":{"code":"PYEONIN","label":"편인"}, "count":1, "percentage":14.29}
    // the other 6 types have count 0
  ]
}
```

**Assembly rules**
- The day pillar returns `cheonganSipseong: null` + `cheonganDisplayOverride: "일원"`.
- If `isTimeUnknown=true`, omit the HOUR pillar (aggregate over 6 chars; Sipseong denominator 5).