package cn.qfys521.xiaoming.sakura.command.omikuji

import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

class OmikujiGenerator(
    private val salt: String,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    fun generate(
        userId: Long,
        date: LocalDate = LocalDate.now(zoneId)
    ): OmikujiResult {

        fun seed(key: String, d: LocalDate): Long {
            val input = "$userId|$d|$salt|$key"
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(input.toByteArray())
            return hash.take(8).fold(0L) { acc, b ->
                (acc shl 8) or (b.toLong() and 0xff)
            }
        }

        fun today(key: String, min: Int, max: Int): Int =
            Random(seed(key, date)).nextInt(min, max + 1)

        // ensure yesterday does not produce the removed "NONE" (value 12)
        fun yesterday(key: String): Int =
            Random(seed(key, date.minusDays(1))).nextInt(1, 11 + 1)

        return OmikujiResult(
            health = today("dailydiv1", 1, 11),
            love = today("dailydiv2", 1, 11),
            family = today("dailydiv3", 1, 11),
            friend = today("dailydiv4", 1, 11),
            career = today("dailydiv5", 1, 11),
            fame = today("dailydiv6", 1, 11),
            wealth = today("dailydiv7", 1, 11),

            healthY = yesterday("dailydiv1"),
            loveY = yesterday("dailydiv2"),
            familyY = yesterday("dailydiv3"),
            friendY = yesterday("dailydiv4"),
            careerY = yesterday("dailydiv5"),
            fameY = yesterday("dailydiv6"),
            wealthY = yesterday("dailydiv7"),

            point = today("dailydiv8", 6, 36),

            s1 = today("dailydiv1S", 2, 7),
            s2 = today("dailydiv2S", 2, 6),
            s3 = today("dailydiv3S", 2, 8),
            s4 = today("dailydiv4S", 2, 7),
            s5 = today("dailydiv5S", 2, 7)
        )
    }
}
