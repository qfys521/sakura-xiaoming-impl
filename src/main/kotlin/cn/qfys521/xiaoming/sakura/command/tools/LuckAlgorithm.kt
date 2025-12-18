package cn.qfys521.xiaoming.sakura.command.tools

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LuckAlgorithm {

    fun get(identifier: Long, key: String?): Int {
        return get(Date(), identifier, key)
    }

    fun get(date: Date?, identifier: Long, key: String?): Int {
        return get(getDay(date), identifier, key)
    }

    fun get(day: Int, identifier: Long, key: String?): Int {
        val code = rfc4226(getSeed(day, identifier), key, 8)
        // 返回值是均匀分布的1到100
        return code % 101
    }

    fun getDay(date: Date?): Int {
        val calendar: Calendar = Calendar.getInstance()
        if (date != null) calendar.setTime(date)
        val year: Int = calendar.get(Calendar.YEAR) - 1
        var day = year * 365 + year / 4 - year / 100 + year / 400 // 闰年调整
        day += calendar.get(Calendar.DAY_OF_YEAR)
        return day
    }

    // 二进制拼接日期与QQ号，生成64bit种子，范围约为公元45900年和12位QQ号
    fun getSeed(day: Int, identifier: Long): Long {
        return (identifier and 0x000000FFFFFFFFFFL) or ((day.toLong()) shl 40)
    }

    @JvmOverloads
    fun rfc4226(seed: Long, key: String?, digits: Int = 6): Int {
        return rfc4226(seed, Base64.getDecoder().decode(key), digits)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun rfc4226(seed: Long, key: ByteArray, digits: Int): Int {
        var digits = digits
        val seedBytes = byteArrayOf(
            (seed shr 56).toByte(),
            (seed shr 48).toByte(),
            (seed shr 40).toByte(),
            (seed shr 32).toByte(),
            (seed shr 24).toByte(),
            (seed shr 16).toByte(),
            (seed shr 8).toByte(),
            (seed).toByte(),
        )
        try {

            val keySpec = SecretKeySpec(key, "HMACSHA1")
            val mac: Mac = Mac.getInstance("HMACSHA1")
            mac.init(keySpec)
            val hash: ByteArray = mac.doFinal(seedBytes)
            val index = hash[19].toInt() and 0xF
            var code = 0
            for (i in 0..3) {
                var num = hash[index + i].toInt()
                if (num < 0) num += 256
                if (i == 0) num = num and 0x7F
                code = num or (code shl 8)
            }
            if (digits > 9) digits = 9
            if (digits < 1) digits = 1
            var divider = 10
            for (i in 1..<digits) divider *= 10
            code %= divider
            return code
        } catch (ex: IllegalArgumentException) {
            return -1
        } catch (ex: NoSuchAlgorithmException) {
            return -1
        } catch (ex: InvalidKeyException) {
            return -1
        }
    }
}