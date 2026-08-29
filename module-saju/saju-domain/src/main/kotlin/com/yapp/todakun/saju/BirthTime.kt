package com.yapp.todakun.saju

/**
 * 십이시진(十二時辰) - 2시간 단위 전통 시간 구분. [branch]는 사용자가 입력한 시진의 지지(시주 지지).
 * 진태양시 분 단위 보정은 도입하지 않고 시진 단위를 그대로 사용한다(정책: 십이시진 재사용).
 * [UNKNOWN]은 출생시간 모름 → 입력된 시진이 없어 [branch]는 null이지만,
 * 계산에는 00:00이 속한 자시를 대입해 시주까지 산출한다([calculationBranch]).
 */
enum class BirthTime(
    val branch: EarthlyBranch?,
) {
    JASI(EarthlyBranch.JA), // 23:30~01:29 (자시)
    CHUKSI(EarthlyBranch.CHUK), // 01:30~03:29 (축시)
    INSI(EarthlyBranch.IN), // 03:30~05:29 (인시)
    MYOSI(EarthlyBranch.MYO), // 05:30~07:29 (묘시)
    JINSI(EarthlyBranch.JIN), // 07:30~09:29 (진시)
    SASI(EarthlyBranch.SA), // 09:30~11:29 (사시)
    OSI(EarthlyBranch.O), // 11:30~13:29 (오시)
    MISI(EarthlyBranch.MI), // 13:30~15:29 (미시)
    SINSI(EarthlyBranch.SIN), // 15:30~17:29 (신시)
    YUSI(EarthlyBranch.YU), // 17:30~19:29 (유시)
    SULSI(EarthlyBranch.SUL), // 19:30~21:29 (술시)
    HAESI(EarthlyBranch.HAE), // 21:30~23:29 (해시)
    UNKNOWN(null), // 시간 모름 → 00:00(자시) 기준으로 계산
    ;

    /**
     * 만세력 계산에 대입할 시진 지지. 시간 모름([UNKNOWN])이면 00:00이 속한 자시(子時)로 대체해
     * 시주까지 계산한다(시주가 빠진 반쪽 명식 대신 8글자 명식을 내려주기 위한 정책).
     * 저장·응답에는 [UNKNOWN]과 `isTimeUnknown=true`가 그대로 남으므로 "시간 모름"은 계속 구분된다.
     */
    val calculationBranch: EarthlyBranch
        get() = branch ?: EarthlyBranch.JA
}
