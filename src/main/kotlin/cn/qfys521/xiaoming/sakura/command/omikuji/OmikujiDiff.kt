package cn.qfys521.xiaoming.sakura.command.omikuji

enum class OmikujiDiff {
    UP, DOWN, SAME, NONE;

    companion object {
        fun compare(today: Int, yesterday: Int): OmikujiDiff {
            if (yesterday == 12) return NONE
            return when {
                today < yesterday -> UP
                today > yesterday -> DOWN
                else -> SAME
            }
        }
    }
}
