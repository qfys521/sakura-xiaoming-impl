package cn.qfys521.xiaoming.sakura.config

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Base64

data class JrrpConfig(
    @field:JsonProperty("key")
    var key: String = Base64.getEncoder().encodeToString("jrrp-key".toByteArray())
)