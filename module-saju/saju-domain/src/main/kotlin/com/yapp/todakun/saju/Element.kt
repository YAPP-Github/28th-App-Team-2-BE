package com.yapp.todakun.saju

/** 오행(五行). [label] 한글 독음, [hanja] 한자. 상생/상극 관계로 십성을 판정한다. */
enum class Element(
    val label: String,
    val hanja: String,
) {
    WOOD("목", "木"),
    FIRE("화", "火"),
    EARTH("토", "土"),
    METAL("금", "金"),
    WATER("수", "水"),
    ;

    /** 상생(相生): 이 오행이 生하는 다음 오행. 木→火→土→金→水→木 */
    fun generates(): Element =
        when (this) {
            WOOD -> FIRE
            FIRE -> EARTH
            EARTH -> METAL
            METAL -> WATER
            WATER -> WOOD
        }

    /** 상극(相剋): 이 오행이 剋하는 오행. 木剋土, 土剋水, 水剋火, 火剋金, 金剋木 */
    fun controls(): Element =
        when (this) {
            WOOD -> EARTH
            EARTH -> WATER
            WATER -> FIRE
            FIRE -> METAL
            METAL -> WOOD
        }
}
