package cn.qfys521.xiaoming.sakura.config

data class ChatConfig(

    /** Codex 使用的模型；留空时使用 Codex CLI 的默认模型。 */
    var modelName: String = "",

    /** 兼容旧版配置保留，采样参数由 Codex CLI 管理。 */
    var temperature: Float = 0.7f,

    /** 兼容旧版配置保留，输出预算由 Codex CLI 管理。 */
    var maxTokens: Int = 4096,

    /** 兼容旧版配置保留，采样参数由 Codex CLI 管理。 */
    var topP: Double = 1.0,

    /** 可选的 OpenAI-compatible Responses API Key。留空时使用 Codex CLI 登录态。 */
    var token: String = "",

    var systemPrompt: String? = null,

    /** 必须是支持 Responses API 的 OpenAI-compatible endpoint。 */
    var apiUrl: String = "",

    var enableSearch: Boolean = true,

    /** Codex CLI 不在 PATH 时填写其可执行文件路径；留空使用默认 PATH 查找。 */
    var codexExecutable: String = ""
)
