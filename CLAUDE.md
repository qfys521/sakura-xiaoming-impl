# sakura-xiaoming-impl

XiaoMing bot plugin - AI chat, fortune, omikuji, ban management.

## Build

```
gradle shadowJar
```

Output: `build/libs/sakura-xiaoming-impl-vX.X.X.jar`

## Version management

Version is stored in two files, must be updated together:

- `build.gradle.kts` - `version = "vX.X.X"`
- `src/main/resources/xiaoming.json` - `"version": "vX.X.X"`

### Tag format rules

| Pattern           | Example            |
|-------------------|--------------------|
| Stable release    | `v1.3.1`           |
| Alpha pre-release | `v1.4.0-alpha1`    |
| Beta pre-release  | `v0.0.1-beta1`     |
| RC pre-release    | `v6.8.0-rc4`       |
| Snapshot          | `v3.4.5-snapshot4` |

### When bumping version: auto git tag

Every time the user asks to bump/update the version number, after updating both files, create an annotated git tag:

```
git tag -a <version> <commit-hash> -m "<version>"
```

- Use the commit where `build.gradle.kts` version was updated
- Tag name = exact version string (e.g. `v1.4.0-alpha3`)
- Always annotated tags (`-a`), not lightweight

## Codex Java SDK

The chat/agent implementation uses `libs/codex-java-sdk-0.0.7.jar` and launches the local
`codex app-server` process. Runtime requirements:

- JDK 25 (the SDK is compiled for Java 25)
- Codex CLI installed and available as `codex`, or set `codexExecutable` in `chat-config.json`
- A Codex CLI login, or an OpenAI-compatible Responses API URL and token in `chat-config.json`

`apiUrl` must support the Responses API; the previous Chat Completions-only endpoint is not
compatible with the SDK. `modelName` may be left empty to use the Codex CLI default model.
Example `chat-config.json` using the Codex CLI login:

```json
{
  "modelName": "",
  "token": "",
  "apiUrl": "",
  "enableSearch": true,
  "codexExecutable": ""
}
```

For an OpenAI-compatible Responses API, set `apiUrl` to its `/v1` endpoint and provide
`token`. Existing configurations that still point to the old DashScope Chat Completions endpoint
must be updated manually.
