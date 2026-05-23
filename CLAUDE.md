# sakura-xiaoming-impl

XiaoMing bot plugin — AI chat, fortune, omikuji, ban management.

## Build

```
gradle shadowJar
```

Output: `build/libs/sakura-xiaoming-impl-vX.X.X.jar`

## Version management

Version is stored in two files, must be updated together:

- `build.gradle.kts` — `version = "vX.X.X"`
- `src/main/resources/xiaoming.json` — `"version": "vX.X.X"`

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
