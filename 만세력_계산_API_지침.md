# 만세력 계산 API 지침 문서

**대상 서비스**: Todakun (토닥이)
**작성 범위**: 사주 명식(4주) 계산, 오행/십성/십이운성 산출, 궁합 비교
**작성 원칙**: API는 클라이언트가 별도 매핑 없이 화면에 바로 표시할 수 있는 값(한자, 한글 독음, 한글 라벨)을 직접 반환한다.

**기반 라이브러리**: [`urstory/manseryeok-js`](https://github.com/urstory/manseryeok-js) (`@fullstackfamily/manseryeok`, MIT License) 를 Kotlin으로 포팅하여 사용한다. 아래 내용은 이 포팅을 전제로 작성되었다.

---

## 1. 개요

본 API는 사용자의 생년월일시를 입력받아 사주 명식(년/월/일/시주)을 계산하고, 파생 데이터(오행 분포, 십성 분포, 십이운성)를 산출하여 저장한다. 계산 결과는 재사용을 전제로 DB에 저장한다.

핵심 원칙:
- **음양력 변환·60갑자·24절기 계산은 포팅한 라이브러리가 전담**하고, 우리 서비스 DB는 이를 다시 저장하지 않는다.
- **십성/십이운성 판정(일간 기준 상대 계산)은 라이브러리 범위 밖**이므로 자체 구현한다.
- API 응답은 enum 코드가 아니라 화면 표시용 한자·독음·한글 라벨을 조인/조립해서 반환한다.

---

## 2. 기반 라이브러리 (manseryeok-js) 분석 및 포팅 범위

### 2.1 라이브러리가 제공하는 것

- **데이터 범위**: 1900~2050년, KASI(한국천문연구원) 음양력변환계산 데이터 기반
- **DB 비의존**: 모든 데이터가 비트 패킹으로 압축되어 코드에 내장됨 (원본 11.4MB → 225KB) — 별도 DB 조회 없이 순수 계산으로 동작
- **제공 함수**:
  - `solarToLunar` / `lunarToSolar` — 양력·음력 상호 변환 (윤달 포함)
  - `getGapja` — 특정 날짜의 60갑자(년/월/일주) 계산
  - `calculateSaju(year, month, day, hour?, minute?, options?)` — **사주팔자(4주) 계산, 진태양시 보정 자동 적용**
  - `calculateSajuSimple` — 시간 보정 없이 계산 (경도 보정 스킵용)
  - `getAllSolarTerms` / `getSolarTermByName` / `getSolarTermForDate` / `getSolarTermsByYear` 등 — 24절기 조회
  - `getSajuMonth(month, day)` — 절기 기준 사주월 산출
  - `SIXTY_PILLARS`, `getPillarById`, `getPillarByHangul` — 60갑자별 한글/한자/로마자/오행/음양 데이터
- **진태양시 보정**: `options.longitude`(기본값 127, 서울) 기준으로 표준 자오선(135도)과의 경도 차이를 자동 계산해 시주 계산에 반영. 결과에 `isTimeCorrected`, `correctedTime` 포함.
- **검증 이력**: v1.0.4~v1.0.8에 걸쳐 지지 오행 매핑 오류(진·오·술), 24절기-사주월 매핑 오류(한로~대한 구간), 월주가 음력 월초가 아닌 **절기 기준**으로 계산되도록 하는 수정이 이루어짐. 즉 우리가 지난 문서에서 우려했던 "월주 절기 기준 계산", "60갑자 오행 매핑" 이슈가 이미 회귀 테스트(월주 26개, 절기-사주월 33개)로 검증되어 있다.

### 2.2 라이브러리가 다루지 않는 것 (자체 구현 필요 영역)

| 항목 | 라이브러리 지원 여부 | 처리 방안 |
|---|---|---|
| 십성(十星) 판정 | ❌ 미지원 | 일간 기준 자체 로직 구현 |
| 십이운성(포태법) 판정 | ❌ 미지원 | 지지 기준 자체 로직 구현 |
| 오행 분포/십성 분포 집계 | ❌ 미지원 | 4주 결과를 받아 자체 집계 |
| 자시/야자시 분리 정책 | ❌ 미지원(단일 처리) | 필요 시 보정된 시각을 받아 자체 후처리 |
| **역사적 표준시 기준 변경** (예: 1908~1911년 동경 127도30분 사용 이력) | ❌ 미지원 (항상 135도 기준 경도 보정만 수행) | 1912년 이전 출생자 계산 정확도에 영향 — **정책 결정 필요** (아래 8절 참고) |
| 궁합(합충형파해) 판정 | ❌ 미지원 | 자체 구현 |

### 2.3 결과적으로 삭제되는 것

- **`solar_term` 테이블 삭제**: 24절기 데이터는 더 이상 우리 DB에 적재하지 않는다. 라이브러리 내장 데이터를 그대로 조회한다. KASI API를 통한 별도 수집 파이프라인도 불필요해진다.
- **`code_heavenly_stem`, `code_earthly_branch`, `code_ohaeng` 테이블 삭제**: 천간·지지의 한자/독음/오행 매핑은 라이브러리의 `SIXTY_PILLARS`/`getPillarById` 데이터로 대체한다. 우리가 별도 마스터를 관리할 이유가 없다.
- **`standard_time_period` 테이블은 보류(조건부 유지)**: 라이브러리가 다루지 않는 영역이라 완전히 대체되지 않는다. 8절 정책 결정에 따라 유지 또는 삭제.

---

## 3. 아키텍처

```
saju-core (순수 Kotlin, 프레임워크 의존성 없음)
 ├─ manseryeok/         manseryeok-js를 Kotlin으로 포팅한 모듈
 │   ├─ SolarLunarConverter   (solarToLunar/lunarToSolar 대응)
 │   ├─ GapjaCalculator       (getGapja, SIXTY_PILLARS 대응)
 │   └─ SajuCalculator        (calculateSaju/calculateSajuSimple 대응)
 ├─ sipseong/           일간 기준 십성 판정 (자체 구현)
 ├─ sibiunseong/        지지 기준 십이운성 판정 (자체 구현)
 └─ ganghap/            궁합 합충형파해 판정 (자체 구현)

saju-app (Spring Boot)
 ├─ Service: saju-core 호출 → 계산 결과를 saju_chart/saju_pillar/saju_ohaeng/saju_sipseong에 저장
 ├─ Presenter/Mapper: enum 코드 + 라이브러리 데이터를 조합해 화면 표시용 DTO로 변환
 └─ API 컨트롤러
```

이전 설계 대비 가장 큰 변화는 `solarterm/`, `timeadjust/` 모듈이 **포팅된 라이브러리 호출로 대체**된다는 점이다. 우리가 직접 구현/검증해야 했던 절기 계산과 진태양시 보정 로직 대부분이 이미 검증된 코드로 대체되므로, 자체 구현 범위는 십성·십이운성·궁합 판정으로 좁아진다.

---

## 4. 계산 처리 순서 (Pipeline)

1. 입력 검증 (생년월일시, 양/음력, 윤달, 성별, 시간 모름 여부)
2. 음력 입력 시 `lunarToSolar`로 양력 변환
3. `calculateSaju(year, month, day, hour, minute, { longitude, applyTimeCorrection })` 호출 → 4주(년/월/일/시) + 보정된 시각(`correctedTime`) 획득
   - 시간 모름이면 `calculateSajuSimple` 또는 `hour` 파라미터 생략 형태로 호출해 시주 제외
4. 반환된 4주 각각에 대해 십성 판정 (일간 기준, 일주 천간은 "일원" 처리)
5. 반환된 4주 각각의 지지에 대해 십이운성 판정
6. 오행 산출 및 집계 (라이브러리의 `element` 필드 활용)
7. 십성 산출 집계
8. 결과 저장: `saju_chart` + `saju_pillar` + `saju_ohaeng` + `saju_sipseong`
9. 응답 조립 시점에 라이브러리 데이터(한자/독음) + `code_sipseong`/`code_sibiunseong` 마스터를 조합해 표시값 포함 DTO 반환

---

## 5. DB 스키마

> ⚠️ **`member` 테이블 관련 확인 필요**: 실제 파일에서 `member`가 `birth_date`/`birth_time`/`calendar_type`/`gender`를 직접 갖고 있는데, 이 정보는 이미 `saju_chart`가 담고 있는 것과 동일하다. 회원 본인의 사주도 `saju_chart`(`user_id`=해당 회원, `is_self=true`)로 저장하는 구조라면, `member`에 생년월일 관련 컬럼을 중복으로 둘 필요가 없다. 회원가입 시점에 본인 사주 계산을 `saju_chart`에 1건 생성하고, `member`에서는 인증/프로필 관련 컬럼만 유지하는 방향을 권장한다 (단, 회원가입 플로우 설계가 아직 확정 전이면 8절 정책 항목으로 남겨둔다).

### 5.1 원천 데이터 — 대폭 축소

```sql
CREATE TABLE `standard_time_period` (
  `id` BINARY(16) NOT NULL COMMENT 'UUIDv7 기본키',
  `start_date` DATE NOT NULL COMMENT '해당 표준시 기준이 적용되기 시작한 날짜',
  `end_date` DATE NOT NULL COMMENT '해당 표준시 기준의 마지막 날짜 (진행중이면 9999-12-31)',
  `utc_offset_minutes` SMALLINT NOT NULL COMMENT 'UTC 대비 오프셋(분). 예: 동경135도=+540, 동경127도30분=+510',
  `description` VARCHAR(100) NULL COMMENT '이 구간의 유래/설명 (예: 동경127도30분 한성 표준시)',
  PRIMARY KEY (`id`)
) COMMENT='한국 표준시 변경 이력. manseryeok-js는 항상 135도 기준 경도 보정만 수행하므로, 1912년 이전 출생자를 정밀 지원할지 여부에 따라 유지/삭제 결정 (8절 참고)';
```

> `solar_term`, `code_heavenly_stem`, `code_earthly_branch`, `code_ohaeng` 테이블은 **삭제**한다. 해당 데이터는 포팅된 `manseryeok` 모듈에서 직접 조회한다.

### 5.2 코드 마스터 — DB 테이블 대신 Kotlin Enum으로 처리

십성(10종)·십이운성(12종)도 천간/지지와 마찬가지로 고정된 명리학 상수라, 별도 DB 마스터 테이블 없이 Kotlin enum에 라벨을 직접 붙여 관리한다.

```kotlin
enum class Sipseong(val label: String) {
    BIGYEON("비견"),   // 일간과 오행·음양이 모두 같음 — 형제/동료, 자립·경쟁의 기운
    GEOPJAE("겁재"),   // 일간과 오행은 같으나 음양이 다름 — 경쟁·재물 손실 가능성의 기운
    SIKSIN("식신"),    // 일간이 생(生)하고 음양이 같음 — 표현력·먹을 복, 온화한 창작의 기운
    SANGGWAN("상관"),  // 일간이 생(生)하고 음양이 다름 — 재능·비판, 반항적 표현의 기운
    PYEONJAE("편재"),  // 일간이 극(剋)하고 음양이 같음 — 유동적 재물, 사업·투기의 기운
    JEONGJAE("정재"),  // 일간이 극(剋)하고 음양이 다름 — 안정적 재물, 성실한 축적의 기운
    PYEONGWAN("편관"), // 일간을 극(剋)하고 음양이 같음 — 억압·통솔, 강한 압박의 기운 (일명 칠살)
    JEONGGWAN("정관"), // 일간을 극(剋)하고 음양이 다름 — 명예·직위, 원칙과 책임의 기운
    PYEONIN("편인"),   // 일간을 생(生)하고 음양이 같음 — 편중된 학습·직관, 독특한 사고의 기운
    JEONGIN("정인")    // 일간을 생(生)하고 음양이 다름 — 정통 학문·인덕, 안정적 지원의 기운
}

enum class Sibiunseong(val label: String) {
    JANGSAENG("장생"), // 태어남 — 새로운 시작, 성장의 초입
    MOKYOK("목욕"),    // 씻김 — 미성숙·유혹, 불안정한 성장기
    GWANDAE("관대"),   // 관을 씀(성인식) — 사회 진출 준비, 패기
    GEONROK("건록"),   // 녹을 세움 — 자립·전성기 진입, 실무 능력의 절정
    JEWANG("제왕"),    // 왕이 됨 — 기운이 가장 강한 절정기
    SOE("쇠"),         // 쇠퇴 시작 — 절정 이후 완만한 하강
    BYEONG("병"),      // 병듦 — 기운 약화, 신중함이 필요한 시기
    SA("사"),          // 죽음 — 활동력 정지, 내면화의 시기
    MYO("묘"),         // 무덤에 들어감 — 침잠·저장, 휴식과 정리
    JEOL("절"),        // 기운이 끊김 — 완전한 침체, 다음 순환 대기
    TAE("태"),         // 잉태됨 — 새 기운이 움트기 시작
    YANG("양")         // 자라남 — 태아처럼 조용히 성장하는 준비기
}
```

`saju_pillar`/`saju_sipseong`에는 DB 네이티브 `ENUM` 타입으로 코드값만 저장하고, 라벨/의미는 API 응답 조립 시 위 Kotlin enum에서 바로 조회한다. DB 테이블/FK가 없으므로 마이그레이션이나 시드 데이터 적재 작업도 불필요하다.

### 5.3 계산 결과

```sql
CREATE TABLE `saju_chart` (
  `id` BINARY(16) NOT NULL COMMENT 'UUIDv7 기본키',
  `user_id` BINARY(16) NULL COMMENT '이 차트를 생성(입력)한 계정 ID (member.id 참조). 비로그인 계산은 NULL. ※ NOT NULL로 두면 파트너/비회원 입력이 불가능해지니 반드시 NULL 허용 유지',
  `is_self` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'TRUE=user_id 본인의 사주, FALSE=user_id가 타인(궁합 상대방 등)의 정보를 대신 입력한 사주. "내 사주 목록" 조회 시 필터링 기준',
  `name` VARCHAR(50) NULL COMMENT '토닥이, 토실이 등 대상 이름 (화면 표시용)',
  `gender` ENUM('MALE','FEMALE') NOT NULL COMMENT '성별, 대운 순행/역행 판정에 사용',
  `input_calendar_type` ENUM('SOLAR','LUNAR') NOT NULL COMMENT '사용자가 입력한 달력 종류 (양력/음력)',
  `input_date` DATE NOT NULL COMMENT '사용자가 입력한 원본 생년월일 (보정 전)',
  `input_time` TIME NULL COMMENT '사용자가 입력한 원본 출생시간 (보정 전), 시간 모름이면 NULL. ※ 8절 "십이시진 vs 정확한 시간" 정책 결정과 연동',
  `is_leap_month` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '음력 입력 시 윤달 여부',
  `is_time_unknown` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '출생시간 모름 여부, TRUE면 시주 계산 생략',
  `birth_longitude` DECIMAL(6,3) NULL COMMENT 'calculateSaju의 options.longitude로 전달, 미입력 시 라이브러리 기본값(127, 서울) 사용',
  `is_time_corrected` BOOLEAN NOT NULL COMMENT '진태양시 보정 적용 여부, 라이브러리 응답의 isTimeCorrected',
  `corrected_hour` TINYINT NULL COMMENT '진태양시 보정 후 시(時), 라이브러리 응답의 correctedTime.hour',
  `corrected_minute` TINYINT NULL COMMENT '진태양시 보정 후 분(分), 라이브러리 응답의 correctedTime.minute',
  `solar_term_name` VARCHAR(20) NULL COMMENT '월주 산정에 적용된 절기명 (라이브러리 조회 결과 스냅샷, FK 아님)',
  `day_master_stem` VARCHAR(10) NOT NULL COMMENT '일간(일주 천간) 코드, 모든 십성 판정의 기준값. 라이브러리 SIXTY_PILLARS 코드값',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '계산 결과 생성 일시',
  PRIMARY KEY (`id`)
) COMMENT='사주 명식 계산 결과 헤더. 본인 입력과 타인(궁합 상대방 등) 입력을 모두 이 테이블 하나로 처리 (is_self로 구분)';

ALTER TABLE `saju_chart` ADD CONSTRAINT `fk_saju_chart_member` FOREIGN KEY (`user_id`) REFERENCES `member`(`id`);

CREATE TABLE `saju_pillar` (
  `id` BINARY(16) NOT NULL COMMENT 'UUIDv7 기본키',
  `chart_id` BINARY(16) NOT NULL COMMENT '소속 사주 명식 (saju_chart.id)',
  `pillar_type` ENUM('YEAR','MONTH','DAY','HOUR') NOT NULL COMMENT '기둥 종류 (년주/월주/일주/시주)',
  `heavenly_stem` VARCHAR(10) NOT NULL COMMENT '천간 코드 (갑을병정무기경신임계). 라이브러리 SIXTY_PILLARS 코드값 (FK 아님, 코드에서 직접 조회)',
  `earthly_branch` VARCHAR(10) NOT NULL COMMENT '지지 코드 (자축인묘진사오미신유술해). 라이브러리 SIXTY_PILLARS 코드값',
  `cheongan_sipseong` ENUM('BIGYEON','GEOPJAE','SIKSIN','SANGGWAN','PYEONJAE',
                        'JEONGJAE','PYEONGWAN','JEONGGWAN','PYEONIN','JEONGIN') NULL
    COMMENT '천간 십성 (일간 기준 판정). 각 값의 의미는 5.2절 참고. 일주는 일원이라 NULL',
  `jiji_sipseong` ENUM('BIGYEON','GEOPJAE','SIKSIN','SANGGWAN','PYEONJAE',
                        'JEONGJAE','PYEONGWAN','JEONGGWAN','PYEONIN','JEONGIN') NOT NULL
    COMMENT '지지 십성 (일간 기준 판정). 각 값의 의미는 5.2절 참고',
  `sibiunseong` ENUM('JANGSAENG','MOKYOK','GWANDAE','GEONROK','JEWANG',
                        'SOE','BYEONG','SA','MYO','JEOL','TAE','YANG') NOT NULL
    COMMENT '십이운성 (지지 기준 포태법 판정). 각 값의 의미는 5.2절 참고',
  PRIMARY KEY (`id`)
) COMMENT='4주별 상세 정보. 천간/지지는 라이브러리 데이터 참조값(FK 없음), 십성/십이운성은 DB 네이티브 ENUM(Kotlin enum과 1:1 대응, 별도 마스터 테이블 없음)';

ALTER TABLE `saju_pillar` ADD CONSTRAINT `fk_saju_pillar_chart` FOREIGN KEY (`chart_id`) REFERENCES `saju_chart`(`id`);

CREATE TABLE `saju_ohaeng` (
  `id` BINARY(16) NOT NULL COMMENT 'UUIDv7 기본키',
  `chart_id` BINARY(16) NOT NULL COMMENT '소속 사주 명식 (saju_chart.id)',
  `element` ENUM('WOOD','FIRE','EARTH','METAL','WATER') NOT NULL COMMENT '오행 종류 (목화토금수)',
  `count` TINYINT NOT NULL COMMENT '8글자(또는 시간 모름 시 6글자) 중 해당 오행에 속하는 글자 수',
  `percentage` DECIMAL(5,2) NOT NULL COMMENT '전체 글자 수 대비 비율(%)',
  PRIMARY KEY (`id`)
) COMMENT='오행 분포 집계, 항상 5행';

CREATE TABLE `saju_sipseong` (
  `id` BINARY(16) NOT NULL COMMENT 'UUIDv7 기본키',
  `chart_id` BINARY(16) NOT NULL COMMENT '소속 사주 명식 (saju_chart.id)',
  `sipseong_type` ENUM('BIGYEON','GEOPJAE','SIKSIN','SANGGWAN','PYEONJAE',
                        'JEONGJAE','PYEONGWAN','JEONGGWAN','PYEONIN','JEONGIN') NOT NULL
    COMMENT '십성 종류. 각 값의 의미는 5.2절 참고',
  `count` TINYINT NOT NULL COMMENT '일간을 제외한 나머지 글자 중 해당 십성으로 판정된 개수 (분모는 8절 정책 확정 필요)',
  `percentage` DECIMAL(5,2) NOT NULL COMMENT '전체 판정 대상 글자 수 대비 비율(%)',
  PRIMARY KEY (`id`)
) COMMENT='십성 분포 집계, 항상 10행 (0건 포함)';

ALTER TABLE `saju_ohaeng` ADD CONSTRAINT `fk_saju_ohaeng_chart` FOREIGN KEY (`chart_id`) REFERENCES `saju_chart`(`id`);
ALTER TABLE `saju_sipseong` ADD CONSTRAINT `fk_saju_sipseong_chart` FOREIGN KEY (`chart_id`) REFERENCES `saju_chart`(`id`);
```

> `heavenly_stem`/`earthly_branch`/`cheongan_sipseong`/`jiji_sipseong`/`sibiunseong` 모두 DB 마스터 테이블에 대한 FK가 없다. 천간/지지는 포팅된 라이브러리 데이터, 십성/십이운성은 Kotlin enum이 각각 정합성의 기준(source of truth)이며, DB `ENUM` 타입 자체가 값의 범위를 제약한다.

### 5.4 궁합 비교

> ⚠️ **실제 파일에서 `saju_compatibility_ganghap`의 PK가 `id`가 아니라 `description`으로 잘못 지정되어 있었다.** `description`은 NULL 허용 텍스트라 PK가 될 수 없다 — 아래 DDL대로 `id`를 PK로 사용해야 한다.
> ⚠️ **`Partner` 테이블은 삭제한다.** 파트너 정보는 5.3절의 `saju_chart`(`is_self=false`)로 들어가고, 관계 유형(연인/친구 등)은 `Partner.relationship`이 아니라 아래 `saju_compatibility.relationship_type`으로 통합한다. 두 곳에 관계 유형을 중복해서 둘 이유가 없다.
> 컬럼명은 실제 파일에서 이미 쓰고 있는 `my_chart_id`/`partner_chart_id`로 통일한다 (이전 초안의 `chart_id_a`/`chart_id_b`보다 의미가 명확해 실제 네이밍을 채택).

```sql
CREATE TABLE `saju_compatibility` (
  `id` BINARY(16) NOT NULL COMMENT 'UUIDv7 기본키',
  `my_chart_id` BINARY(16) NOT NULL COMMENT '내 사주 (saju_chart.id, is_self=true인 차트)',
  `partner_chart_id` BINARY(16) NOT NULL COMMENT '상대방 사주 (saju_chart.id, is_self=false인 차트)',
  `relationship_type` ENUM('LOVER','FRIEND','FAMILY','COLLEAGUE') NOT NULL COMMENT '관계 유형 (연인/친구/가족/동료), 화면 상단 배지에 표시. Partner 테이블의 관계 필드를 흡수',
  `compatibility_score` TINYINT NOT NULL COMMENT '궁합 점수 (0~100), 도넛 그래프에 표시',
  `headline` VARCHAR(50) NOT NULL COMMENT '메인 카피 (예: 함께할 수록 빛나는 궁합)',
  `subheadline` VARCHAR(100) NOT NULL COMMENT '보조 카피 (예: 함께 있을 때, 편안함이 커지는 사이예요)',
  `summary` VARCHAR(200) NOT NULL COMMENT '점수 아래 짧은 요약 설명',
  `total_analysis` TEXT NOT NULL COMMENT '궁합 총운 분석 장문 텍스트 (AI 생성 해석)',
  `analysis_basis` VARCHAR(50) NOT NULL DEFAULT '사주 팔자 기반' COMMENT '분석 데이터 출처 표기 문구',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '궁합 결과 생성 일시',
  PRIMARY KEY (`id`)
) COMMENT='궁합 비교 결과';

ALTER TABLE `saju_compatibility` ADD CONSTRAINT `fk_compat_my_chart` FOREIGN KEY (`my_chart_id`) REFERENCES `saju_chart`(`id`);
ALTER TABLE `saju_compatibility` ADD CONSTRAINT `fk_compat_partner_chart` FOREIGN KEY (`partner_chart_id`) REFERENCES `saju_chart`(`id`);

CREATE TABLE `saju_compatibility_ohaeng` (
  `id` BINARY(16) NOT NULL COMMENT 'UUIDv7 기본키',
  `compatibility_id` BINARY(16) NOT NULL COMMENT '소속 궁합 비교 결과 (saju_compatibility.id)',
  `element` ENUM('WOOD','FIRE','EARTH','METAL','WATER') NOT NULL COMMENT '오행 종류 (목화토금수)',
  `percentage` DECIMAL(5,2) NOT NULL COMMENT '두 사람의 사주를 합산해 정규화한 비율(%), 5개 합계 100%',
  PRIMARY KEY (`id`)
) COMMENT='두 사람 합산 오행 분포, 항상 5행';

ALTER TABLE `saju_compatibility_ohaeng`
  ADD CONSTRAINT `fk_compat_ohaeng` FOREIGN KEY (`compatibility_id`) REFERENCES `saju_compatibility`(`id`);

CREATE TABLE `saju_compatibility_ganghap` (
  `id` BINARY(16) NOT NULL COMMENT 'UUIDv7 기본키 (※ description이 아니라 반드시 id를 PK로 사용)',
  `compatibility_id` BINARY(16) NOT NULL COMMENT '소속 궁합 비교 결과 (saju_compatibility.id)',
  `relation_type` ENUM('CHEONGAN_HAP','JIJI_HAP','CHUNG','HYEONG','PA','HAE') NOT NULL
    COMMENT '판정된 관계 종류: 천간합/지지합/충/형/파/해',
  `pillar_a_type` ENUM('YEAR','MONTH','DAY','HOUR') NOT NULL COMMENT '내 사주에서 관여한 기둥',
  `pillar_b_type` ENUM('YEAR','MONTH','DAY','HOUR') NOT NULL COMMENT '상대 사주에서 관여한 기둥',
  `description` VARCHAR(100) NULL COMMENT '정임합(丁壬合) 같은 구체 명칭, 총운 분석 텍스트의 근거로 사용',
  PRIMARY KEY (`id`)
) COMMENT='궁합 판정 근거 목록';

ALTER TABLE `saju_compatibility_ganghap`
  ADD CONSTRAINT `fk_compat_ganghap` FOREIGN KEY (`compatibility_id`) REFERENCES `saju_compatibility`(`id`);
```

---

## 6. API 명세

### 6.1 사주 명식 생성

```
POST /api/v1/saju-charts
```

**Request**
```json
{
  "name": "토닥이",
  "gender": "FEMALE",
  "calendarType": "SOLAR",
  "birthDate": "2001-05-30",
  "birthTime": "15:00",
  "isLeapMonth": false,
  "isTimeUnknown": false,
  "birthLongitude": 126.978
}
```

**Response** `201 Created`

```json
{
  "chartId": "uuid",
  "name": "토닥이",
  "gender": "FEMALE",
  "birthDateDisplay": "2001.05.30 양력 · 15시 00분",
  "isTimeCorrected": true,
  "correctedTime": { "hour": 14, "minute": 32 },
  "dayMaster": { "code": "GI", "hanja": "己", "reading": "기" },
  "pillars": [
    {
      "pillarType": "HOUR",
      "cheonganSipseong": { "code": "SIKSIN", "label": "식신" },
      "heavenlyStem": { "code": "SIN", "hanja": "辛", "reading": "신" },
      "heavenlyStemElement": { "code": "METAL", "hanja": "金" },
      "earthlyBranch": { "code": "MI", "hanja": "未", "reading": "미" },
      "earthlyBranchElement": { "code": "EARTH", "hanja": "土" },
      "jijiSipseong": { "code": "BIGYEON", "label": "비견" },
      "sibiunseong": { "code": "GWANDAE", "label": "관대" }
    },
    {
      "pillarType": "DAY",
      "cheonganSipseong": null,
      "cheonganDisplayOverride": "일원",
      "heavenlyStem": { "code": "GI", "hanja": "己", "reading": "기" },
      "heavenlyStemElement": { "code": "EARTH", "hanja": "土" },
      "earthlyBranch": { "code": "SA", "hanja": "巳", "reading": "사" },
      "earthlyBranchElement": { "code": "FIRE", "hanja": "火" },
      "jijiSipseong": { "code": "BIGYEON", "label": "비견" },
      "sibiunseong": { "code": "GWANDAE", "label": "관대" }
    }
  ],
  "ohaeng": [
    { "element": { "code": "WOOD", "label": "목", "hanja": "木" }, "count": 1, "percentage": 12.5 }
  ],
  "sipseong": [
    { "type": { "code": "BIGYEON", "label": "비견" }, "count": 2, "percentage": 28.57 }
  ]
}
```

**주요 규칙**
- `heavenlyStem`/`earthlyBranch`의 `hanja`/`reading`은 포팅된 `manseryeok` 모듈의 60갑자 데이터에서 직접 조회하여 채운다 (DB 조회 아님)
- `isTimeUnknown=true`이면 `HOUR` pillar 생략, `isTimeCorrected`/`correctedTime`도 `null`
- 일주는 `cheonganSipseong: null` + `cheonganDisplayOverride: "일원"`로 고정 반환

### 6.2 사주 명식 조회

```
GET /api/v1/saju-charts/{chartId}
```
응답 포맷은 6.1과 동일.

### 6.3 궁합 비교 생성 / 조회

```
POST /api/v1/saju-charts              (파트너 정보도 이 엔드포인트로 생성)
POST /api/v1/saju-compatibilities
GET  /api/v1/saju-compatibilities/{compatibilityId}
```

**파트너(상대방) 정보 처리 방식**: `Partner` 테이블은 사용하지 않는다. 궁합 화면에서 상대방 정보를 입력받으면, 6.1의 `POST /api/v1/saju-charts`를 그대로 호출해 `is_self=false`로 저장한다. 이렇게 하면:
- 4주/오행/십성/십이운성 계산 로직이 한 곳(`saju-core`)에만 존재한다
- 파트너 차트도 `partner_chart_id`로 정상적인 `saju_chart` FK 참조가 가능해 `saju_compatibility`와의 관계가 단순해진다
- 원시 입력값(`name`, `input_date`, `input_time`)은 `saju_chart`에 그대로 남아 있어 정보 손실이 없다
- `is_self=false`인 차트는 "내 사주 목록" 조회 쿼리에서 `WHERE is_self = TRUE`로 자동 제외된다
- 관계 유형(연인/친구 등)은 `Partner.relationship`이 아니라 `saju_compatibility.relationship_type`에 저장한다

응답 구조는 이전과 동일 (`compatibilityScore`, `headline`, `chartA`/`chartB`, `ohaeng`, `ganghapList`, `totalAnalysis` 등). `chartA`/`chartB`의 `pillars` 구조는 6.1과 동일한 방식으로 라이브러리 데이터를 조합한다.

---

## 7. 에러 처리 기준

| 상황 | HTTP 상태 | 처리 |
|---|---|---|
| 입력 연도가 라이브러리 지원 범위(1900~2050) 밖 | 422 | `OutOfRangeError` 캐치 후 지원 범위 안내 |
| 유효하지 않은 날짜(윤달 오류 등) | 400 | `InvalidDateError` 캐치 후 안내 |
| chartId/compatibilityId 존재하지 않음 | 404 | - |
| 십성/십이운성 계산 로직이 정의되지 않은 값을 산출 | 500 | Kotlin enum 범위를 벗어난 값은 컴파일 타임/역직렬화 시점에 차단됨, DB `ENUM` 타입이 저장 시점에도 재차 제약 |

---

## 8. 정책 확정 필요 항목 (기획자 협의 대상)

- [ ] **출생시간 입력 방식 (신규, 중요)**: 실제 파일에서 `member`/`Partner` 모두 `birth_time`을 정확한 시:분이 아니라 **십이시진(12개 전통 시진, 2시간 단위) Enum**으로 받고 있다. 반면 manseryeok-js의 진태양시 보정은 분 단위 정밀도를 전제로 동작한다. 십이시진 단위 입력만 받으면: (1) 보정으로 인해 실제로는 다른 시진으로 넘어가야 하는 경계 케이스를 감지할 수 없고, (2) `corrected_hour`/`corrected_minute` 같은 분 단위 보정 필드가 사실상 무의미해진다. 정확한 시:분 입력을 받을지, 십이시진 선택만 지원하고 보정을 포기할지 결정 필요 — 이전 화면 캡처(15시 00분, 11시 00분)에는 분 단위 입력이 있었으므로, 두 입력 경로가 공존하는 것인지도 확인 필요
- [ ] **`member`-`saju_chart` 데이터 중복 해소**: `member`의 생년월일 관련 컬럼을 유지할지, `saju_chart`로 완전히 이관할지
- [ ] **1912년 이전 출생자 지원 여부**: manseryeok-js는 항상 동경 135도 표준 자오선 기준 경도 보정만 수행하며, 한국의 역사적 표준시 변경 이력(1908~1911년 동경 127도30분 사용 등)은 반영하지 않는다. 해당 기간 출생자의 시주 정확도를 별도로 보정할지, 라이브러리 계산값을 그대로 사용할지 결정 필요 → 보정한다면 `standard_time_period` 테이블 유지, 아니면 삭제
- [ ] 자시 처리 정책: 라이브러리는 자시를 분리하지 않고 단일 처리함. 야자시/조자시 구분이 필요하면 라이브러리의 `correctedTime` 결과를 받아 자체 후처리 로직 추가 필요
- [x] 십성 집계 분모 (7 vs 8) → **7 채택** (일간 제외한 나머지 글자; 시주 있으면 천간 3 + 지지 4)
- [ ] 궁합 결과 재생성 시 이력 관리 방식 (덮어쓰기 vs 버전 이력)
- [ ] 시간 모름 입력 시 궁합/십성 분석 제한 범위 안내 문구
- [x] 라이브러리 지원 범위(1900~2050)를 서비스 지원 범위로 그대로 채택할지 여부 → **채택** (범위 밖은 422 `SAJU_YEAR_OUT_OF_RANGE`)

> **[구현 반영 — saju 도메인]**
> - **호출 방식 (REST 아님)**: 만세력 계산은 클라이언트 REST API가 아니라 **다른 도메인이 호출하는 내부 기능**이다. 진입점은 `shared.CreateSajuChartPort`. `saju`에는 REST 계층(`adapter-in`)이 없다(3모듈: `domain`/`application`/`adapter-out`). 6절의 REST 명세는 **폐기** — 아래 두 호출 경로로 대체.
>   - **회원가입**: `auth.SignupService`가 회원 생성 직후 `CreateSajuChartPort.create(userId=회원ID, isSelf=true, ...)`로 본인 사주를 **같은 트랜잭션**에서 계산·저장(전파 REQUIRED로 원자성 보장).
>   - **궁합(추후)**: 상대방 사주를 `isSelf=false`로 계산(같은 포트 재사용).
> - **계층 배치**: 계산+저장 오케스트레이션·트랜잭션 경계이므로 포트 구현은 `saju-application`의 `@CommandService`(`CreateSajuChartService`)에 둔다.
> - **출생시간 입력**: 십이시진(`BirthTime` Enum) 재사용으로 확정. 진태양시 분 단위 보정 미도입 → `birth_longitude`/`is_time_corrected`/`corrected_hour`/`corrected_minute` 컬럼 제외. 시주 지지는 시진에서 직접, 시주 천간은 오서둔(五鼠遁)으로 산출.
> - **`member`-`saju_chart` 중복**: member 미변경. `saju_chart`는 자체 입력 필드 + `user_id`(nullable)만 연동. 컬럼 이관은 후속.
> - **만세력 계산 엔진**: `ManseryeokPort`(outbound)로 격리. **년/월/일/시주 모두 manseryeok-js(KASI)와 전 범위 대조 검증됨**.
>   - **일주**: JDN 60갑자. 앵커 상수는 라이브러리 일주와 **전 범위 55,151일 일치**하도록 확정(offset=49).
>   - **월주·년주(입춘)**: 종전 근사표를 폐기하고 `SolarTermTable`(리소스 `manseryeok/solar-terms.txt`, `getGapja`의 절기별 월지 변경일을 연도별 추출)로 **실제 24절기 기준** 산출. 라이브러리와 55,139/55,151일 일치, 나머지 12일(특정 연도 12/31)은 **라이브러리 자체 버그**라 오히려 이 어댑터가 정확.
>   - **시주**: 일간 + 십이시진 오서둔.
> - **✅ 음력 지원(완료)**: `LunarSolarConverter`가 음력→양력 변환을 담당한다. manseryeok-js v1.0.8의 `solarToLunar`(안정적인 방향)를 **전 구간 역추출**해 만든 음력 월별 시작일/길이 테이블(리소스 `manseryeok/lunar-index.txt`, 음력 1900~2050, 7.5KB)을 사용하며, `solarToLunar`와 **전 구간 55,121일 양방향 일치**를 검증했다(윤달 포함 443개 회귀 샘플 상시 대조). 참고: 라이브러리의 `lunarToSolar`는 ~5,250개 유효 날짜에서 버그로 실패하므로 직접 쓰지 않고 `solarToLunar` 역추출본을 사용한다(1956-12-31 데이터 구멍 1건도 보정). 유효하지 않은 음력 날짜(존재하지 않는 윤달·일수 초과·상한 초과)는 400 `SAJU_INPUT_INVALID`. → 이로써 **음력 회원가입도 정상 동작**한다.
> - **⚠️ 지침 예시 오류 정정**: 6.1 예시의 `2001-05-30`은 **일주 己巳·시주 辛未가 아니라 일주 癸巳·시주 己未**가 맞다(라이브러리 3개 API `getGapja`/`calculateSaju`/`solarToLunar`가 모두 일치, `2000-01-01=戊午`에서 JDN로 재확인). 예시의 십성 값(HOUR/DAY 동일)도 규칙과 불일치하는 placeholder였음. **규칙과 라이브러리를 정본으로 삼고 예시 JSON은 신뢰하지 않음**.
> - **십성/십이운성/궁합**: 5.2절 규칙 기준 자체 구현(십성·십이운성 완료, 궁합 다음 단계).
> - **구현 검증**: 정정된 `2001-05-30 미시` → 년 辛巳·월 癸巳(입하)·일 癸巳·시 己未 회귀 통과. 24절기 경계 ±1일 495개 + 음력 443개 회귀 샘플 상시 대조.

---

## 9. 검증(테스트) 기준

- manseryeok-js README에 명시된 회귀 테스트(월주 절기 기준 26개, 24절기-사주월 매핑 33개)가 포팅 후에도 동일하게 통과하는지 이식 검증
- 절기 경계 전후 수 분 이내 출생 시각 케이스 회귀 테스트
- 진태양시 보정 결과(`correctedTime`)가 원본 라이브러리와 Kotlin 포팅본에서 동일하게 나오는지 대조 테스트
- 일간(10종) × 지지(12종) 조합 십성/십이운성 판정 전수 테스트 (자체 구현 영역이므로 특히 중요)
- 시중 만세력 서비스 결과값 대조 테스트셋
- `code_*` 마스터 조인 결과가 실제 화면 캡처와 1:1 일치하는지 스냅샷 테스트

---

## 10. 관련 DB 엔티티 요약

- `standard_time_period` — 원천 데이터 (유지 여부는 8절 정책 결정 대상)
- `saju_chart`, `saju_pillar`, `saju_ohaeng`, `saju_sipseong` — 개인별 계산 결과
- `saju_compatibility`, `saju_compatibility_ohaeng`, `saju_compatibility_ganghap` — 궁합 비교 결과

> ~~`solar_term`~~, ~~`code_heavenly_stem`~~, ~~`code_earthly_branch`~~, ~~`code_ohaeng`~~ — manseryeok-js 포팅으로 대체되어 삭제됨
> ~~`code_sipseong`~~, ~~`code_sibiunseong`~~ — Kotlin enum(`Sipseong`, `Sibiunseong`)으로 대체되어 삭제됨. DB에는 값 검증용 네이티브 `ENUM` 컬럼만 남음
> ~~`Partner`~~ — `saju_chart.is_self` 컬럼 추가로 대체되어 삭제됨. 관계 유형은 `saju_compatibility.relationship_type`으로 통합
> `member`의 생년월일 관련 컬럼(`birth_date`/`birth_time`/`calendar_type`/`gender`) 유지 여부는 8절 정책 결정 대상 (`saju_chart`와 데이터 중복)

(상세 컬럼 정의는 본 문서 5절 DDL 참조)
