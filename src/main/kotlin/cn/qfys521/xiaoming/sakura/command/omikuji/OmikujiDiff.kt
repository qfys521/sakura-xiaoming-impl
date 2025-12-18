package cn.qfys521.xiaoming.sakura.command.omikuji

enum class OmikujiDiff {
    UP, DOWN, SAME;

    companion object {
        fun compare(today: Int, yesterday: Int): OmikujiDiff {
            return when {
                today < yesterday -> UP
                today > yesterday -> DOWN
                else -> SAME
            }
        }
    }
}
