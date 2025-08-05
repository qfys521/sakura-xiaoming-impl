package cn.qfys521.xiaoming.sakura.config

data class EssentialsConfig(
    var banedUser: List<Long> = emptyList(),
    var banedGroup: List<Long> = emptyList(),
) {
}