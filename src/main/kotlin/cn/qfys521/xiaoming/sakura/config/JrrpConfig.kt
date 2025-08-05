package cn.qfys521.xiaoming.sakura.config

import java.util.Base64
import kotlinx.serialization.Serializable

@Serializable
data class JrrpConfig(
    var key: String = Base64.getEncoder().encodeToString("jrrp-key".toByteArray())
)