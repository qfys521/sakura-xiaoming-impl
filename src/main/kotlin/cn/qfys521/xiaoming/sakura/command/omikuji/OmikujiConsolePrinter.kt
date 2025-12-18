package cn.qfys521.xiaoming.sakura.command.omikuji

object OmikujiConsolePrinter {

    fun print(result: OmikujiResult): String {
        val sb = StringBuilder()

        sb.appendLine("『御神签』")
        sb.appendLine()

        appendLine(sb, "健康", result.healthY, result.health)
        appendLine(sb, "家庭", result.familyY, result.family)
        appendLine(sb, "友谊", result.friendY, result.friend)
        appendLine(sb, "桃花", result.loveY, result.love)
        appendLine(sb, "事业", result.careerY, result.career)
        appendLine(sb, "名望", result.fameY, result.fame)
        appendLine(sb, "财富", result.wealthY, result.wealth)

        sb.appendLine()

        OmikujiMessages.health[result.s1]?.let { sb.appendLine(it) }
        OmikujiMessages.family[result.s3]?.let { sb.appendLine(it) }
        OmikujiMessages.friend[result.s4]?.let { sb.appendLine(it) }
        OmikujiMessages.love[result.s2]?.let { sb.appendLine(it) }
        OmikujiMessages.career[result.s5]?.let { sb.appendLine(it) }

        return sb.toString()
    }

    private fun appendLine(
        sb: StringBuilder,
        title: String,
        yesterday: Int,
        today: Int
    ) {
        val y = OmikujiLabel.from(yesterday).text
        val t = OmikujiLabel.from(today).text
        sb.appendLine("  $title：  $y >> $t")
    }
}

