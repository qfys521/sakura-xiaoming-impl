package cn.qfys521.xiaoming.sakura.command.services

import cn.qfys521.xiaoming.sakura.config.ChatConfig
import io.github.majiajustar.codex.CodexClient
import io.github.majiajustar.codex.CodexClientConfig
import io.github.majiajustar.codex.CodexThread
import io.github.majiajustar.codex.model.OpenAiCompatibleProviderConfig
import io.github.majiajustar.codex.thread.ThreadOptions
import org.slf4j.Logger
import java.io.File
import java.net.URI
import java.nio.file.Path

/**
 * AI chat service backed by the Codex Java SDK.
 * The SDK owns the local `codex app-server` process and exposes Codex native tools.
 */
class CodexChatService(
    private val config: ChatConfig,
    private val workspace: File,
    private val logger: Logger
) : AutoCloseable {
    private var clientInstance: CodexClient? = null
    private val client: CodexClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createClient().also { clientInstance = it }
    }

    fun startThread(developerInstructions: String, model: String?): CodexThread {
        val options = ThreadOptions.builder()
            .workingDirectory(workspace.toPath().toAbsolutePath().normalize())
            .sandbox(ThreadOptions.Sandbox.WORKSPACE_WRITE)
            .approvalPolicy(ThreadOptions.ApprovalPolicy.NEVER)
            .developerInstructions(developerInstructions)
            .skipGitRepoCheck(true)
            .apply {
                if (!model.isNullOrBlank()) model(model)
            }
            .build()
        return client.startThread(options)
    }

    fun run(thread: CodexThread, prompt: String): String? {
        return try {
            thread.run(prompt).finalResponse()?.trim()
        } catch (e: Exception) {
            logger.error("Codex turn failed", e)
            null
        }
    }

    override fun close() {
        clientInstance?.let {
            try {
                it.close()
            } catch (e: Exception) {
                logger.warn("Failed to close Codex client", e)
            }
        }
    }

    private fun createClient(): CodexClient {
        val builder = CodexClientConfig.builder()
            .workingDirectory(workspace.toPath().toAbsolutePath().normalize())
            .environment("CODEX_HOME", File(workspace, "codex").absolutePath)
            .webSearch(
                if (config.enableSearch) CodexClientConfig.WebSearchMode.LIVE
                else CodexClientConfig.WebSearchMode.DISABLED
            )
            .approvalPolicy(ThreadOptions.ApprovalPolicy.NEVER)
            .workspaceNetworkAccess(config.enableSearch)

        if (config.codexExecutable.isNotBlank()) {
            builder.codexExecutable(Path.of(config.codexExecutable))
        }
        if (config.apiUrl.isNotBlank()) {
            val provider = OpenAiCompatibleProviderConfig.builder("sakura")
                .name("Sakura XiaoMing")
                .baseUrl(URI.create(config.apiUrl.trim()))
                .build()
            builder.openAiCompatibleProvider(provider)
        }
        if (config.token.isNotBlank()) {
            builder.apiKey(config.token)
            // Do not expose the provider key to commands executed by Codex tools.
            builder.configOverride("shell_environment_policy.filters.OPENAI_API_KEY=\"exclude\"")
        }
        if (config.modelName.isNotBlank()) {
            builder.model(config.modelName)
        }
        return CodexClient.create(builder.build())
    }
}
