package com.yapp.todakun.saju

/**
 * 지지(地支) 12지. [hanja] 한자, [reading] 한글 독음, [element] 본기(本氣) 오행, [yinYang] 십성 판정용 음양(용·본기 기준).
 * 음양은 십성 판정을 위해 용(用, 본기)을 기준으로 하며, 자(子)/오(午)는 음, 사(巳)/해(亥)는 양이다(체용 차이).
 * 선언 순서(ordinal)가 60갑자 계산의 지지 인덱스(子=0 … 亥=11)와 일치한다.
 */
enum class EarthlyBranch(
    val hanja: String,
    val reading: String,
    val element: Element,
    val yinYang: YinYang,
) {
    JA("子", "자", Element.WATER, YinYang.YIN),
    CHUK("丑", "축", Element.EARTH, YinYang.YIN),
    IN("寅", "인", Element.WOOD, YinYang.YANG),
    MYO("卯", "묘", Element.WOOD, YinYang.YIN),
    JIN("辰", "진", Element.EARTH, YinYang.YANG),
    SA("巳", "사", Element.FIRE, YinYang.YANG),
    O("午", "오", Element.FIRE, YinYang.YIN),
    MI("未", "미", Element.EARTH, YinYang.YIN),
    SIN("申", "신", Element.METAL, YinYang.YANG),
    YU("酉", "유", Element.METAL, YinYang.YIN),
    SUL("戌", "술", Element.EARTH, YinYang.YANG),
    HAE("亥", "해", Element.WATER, YinYang.YANG),
    ;

    companion object {
        /** 60갑자 지지 인덱스(0..11, 음수·초과분은 순환)로 지지를 조회한다. */
        fun ofIndex(index: Int): EarthlyBranch = entries[((index % entries.size) + entries.size) % entries.size]
    }
}
