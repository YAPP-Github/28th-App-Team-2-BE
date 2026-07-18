package com.yapp.todakun.saju

/**
 * 지지(地支) 12지. [hanja] 한자, [reading] 한글 독음, [element] 본기(本氣) 오행, [yinYang] 음양(위치 기준).
 * 음양은 선언 순서로 자(子)=양, 축(丑)=음이 교대한다(십성 판정용 정음양).
 * 선언 순서(ordinal)가 60갑자 계산의 지지 인덱스(子=0 … 亥=11)와 일치한다.
 */
enum class EarthlyBranch(
    val hanja: String,
    val reading: String,
    val element: Element,
    val yinYang: YinYang,
) {
    JA("子", "자", Element.WATER, YinYang.YANG),
    CHUK("丑", "축", Element.EARTH, YinYang.YIN),
    IN("寅", "인", Element.WOOD, YinYang.YANG),
    MYO("卯", "묘", Element.WOOD, YinYang.YIN),
    JIN("辰", "진", Element.EARTH, YinYang.YANG),
    SA("巳", "사", Element.FIRE, YinYang.YIN),
    O("午", "오", Element.FIRE, YinYang.YANG),
    MI("未", "미", Element.EARTH, YinYang.YIN),
    SIN("申", "신", Element.METAL, YinYang.YANG),
    YU("酉", "유", Element.METAL, YinYang.YIN),
    SUL("戌", "술", Element.EARTH, YinYang.YANG),
    HAE("亥", "해", Element.WATER, YinYang.YIN),
    ;

    companion object {
        /** 60갑자 지지 인덱스(0..11, 음수·초과분은 순환)로 지지를 조회한다. */
        fun ofIndex(index: Int): EarthlyBranch = entries[((index % entries.size) + entries.size) % entries.size]
    }
}
