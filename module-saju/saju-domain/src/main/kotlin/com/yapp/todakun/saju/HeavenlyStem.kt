package com.yapp.todakun.saju

/**
 * 천간(天干) 10간. [hanja] 한자, [reading] 한글 독음, [element] 오행, [yinYang] 음양.
 * 선언 순서(ordinal)가 60갑자 계산의 천간 인덱스(甲=0 … 癸=9)와 일치한다.
 */
enum class HeavenlyStem(
    val hanja: String,
    val reading: String,
    val element: Element,
    val yinYang: YinYang,
) {
    GAP("甲", "갑", Element.WOOD, YinYang.YANG),
    EUL("乙", "을", Element.WOOD, YinYang.YIN),
    BYEONG("丙", "병", Element.FIRE, YinYang.YANG),
    JEONG("丁", "정", Element.FIRE, YinYang.YIN),
    MU("戊", "무", Element.EARTH, YinYang.YANG),
    GI("己", "기", Element.EARTH, YinYang.YIN),
    GYEONG("庚", "경", Element.METAL, YinYang.YANG),
    SIN("辛", "신", Element.METAL, YinYang.YIN),
    IM("壬", "임", Element.WATER, YinYang.YANG),
    GYE("癸", "계", Element.WATER, YinYang.YIN),
    ;

    companion object {
        /** 60갑자 천간 인덱스(0..9, 음수·초과분은 순환)로 천간을 조회한다. */
        fun ofIndex(index: Int): HeavenlyStem = entries[((index % entries.size) + entries.size) % entries.size]
    }
}
