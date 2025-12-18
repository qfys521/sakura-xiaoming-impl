package cn.qfys521.xiaoming.sakura.command.omikuji

enum class OmikujiLabel(val value: Int, val text: String) {
    DAIKICHI(1, "大吉"),
    CHUKICHI(2, "中吉"),
    SHOKICHI(3, "小吉"),
    KICHI(4, "吉"),
    SUEKICHI_A(5, "末吉"),
    SUEKICHI_B(6, "末吉"),
    KYO_A(7, "凶"),
    KYO_B(8, "凶"),
    DAIKYO_A(9, "大凶"),
    DAIKYO_B(10, "大凶"),
    MISHO(11, "未书"),
    NONE(12, "无记录");

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun from(value: Int): OmikujiLabel =
            entries.first { it.value == value }
    }
}
