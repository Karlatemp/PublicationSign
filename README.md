# Publication Sign

A gradle plugin for sign publication artifacts. For fast deploy to maven central

System requirement: `gpg` installed

## Configuration

### Plugin apply

```groovy
plugins {
    // Load this plugin first for resolve magic problems
    id 'io.github.karlatemp.publication-sign' version '1.3.15'
    // other plugins
}
```

In order to avoid strange problems, please let this plugin load **first**

Important: If you are using Kotlin multiplatform.
**Must** apply this plugin before `org.jetbrains.kotlin.multiplatform`

```groovy
plugins {
    id 'io.github.karlatemp.publication-sign'
    id 'org.jetbrains.kotlin.multiplatform'
}
```

### Configuration

`build.gradle`

```groovy
plugins {
    id 'io.github.karlatemp.publication-sign'
}

publicationSign {
    setupWorkflow { workflow ->
        workflow.fastSetup("keys/key.pub", "keys/key.pri")
    }
}

```

`build.gradle.kts`

```kotlin
plugins {
    id("io.github.karlatemp.publication-sign")
}

configure<io.github.karlatemp.publicationsign.PublicationSignExtension> {
    setupWorkflow {
        fastSetup("keys/keys.pub", "keys/keys.pri")
    }
}
```

#### Multi-project configuration

1. Apply this plugin to root project. Then config `PublicationSignExtension`
2. Subprojects only need apply the plugin. Don't need re-setup `PublicationSignExtension`

#### GPG Key setup

See [key-gen.sh](./key-gen.sh)

#### CI
```yaml
name: Publish

jobs:
  # This workflow contains a single job called "build"
  build:
    runs-on: ubuntu-latest
    env:
      WORK_IN_CI: true
    steps:
      - name: Keys setup
        shell: bash
        run: |
          mkdir keys
          echo "$GPG_PUBLIC_" > keys/keys.pub
          echo "$GPG_PRIVATE" > keys/keys.pri
        env:
          GPG_PRIVATE: ${{ secrets.GPG_PRIVATE_KEY }}
          GPG_PUBLIC_: ${{ secrets.GPG_PUBLIC_KEY }}
      - name: Build
        run: ./gradlew build
      - name: Publish
        run: ./gradlew publish

```

## Publish

`./gradlew publish`/ `./gradlew publishToMavenLocal`

-----------------------------------------------------------

Implementation details:

## Local Signing Environment Setup


In `setupWorkflow { fastSetup() }`, arguments that provided to `fastSetup`(`publicKey` & `privateKey`) will be resolved by `Project.file()`.

When both of `publicKey` and `privateKey` exists, PS will run in CI mode, otherwise run in user mode.

> You need setup a personal GPG key when PS running in user mode.
>
> You can use `gpg --list-secret-keys` to find your keys.
>
> References:
> - [GitHub Docs: Generating a new GPG key](https://docs.github.com/en/authentication/managing-commit-signature-verification/generating-a-new-gpg-key)

>
> If you have multiple GPG keys, we recommand you to setup `signing.keyId` in `gradle.properties` of your personal gradle data directory (~/.gradle)
>
> `signing.keyId` can be found by executing `gpg --list-secret-keys`

## CI Env improvement

Sometimes you will receive a `GPG command response 2 != 0` error. It caused by GPG's data directory path length limit.

It means you need your project build with a short path on CI.
> Actually just need use `setupWorkflow { workingDir = new File("/tmp") }` to resolve it

In order to deal with the situation, PS added some options to forcefully override user settings in `build.grade`

- Check system property `publication-sign.workingDir`, force override GPG command working dir if available
- Check system environment variable `PUBLICATION_SIGN_WORKING_DIR`, same as above
- Check system environment variable `NOT_IN_CI`, interrupt if true (Continue to use user mode)
- Check is CI env
  - Check system environment variable `CI`, jump to AATD if true
  - Jump to AAID if system envionment variable `GITHUB_RUN_ID` exists
  - Interrupt if no rule matched (Continue to use user mode)
- Allocate a new temp directory and override user settings (AATD)
  - Calculate project sha1 by project directory path
  - Use `/tmp/ps_$projectsha` if directory creation successed. (Windows) Driver name will be same as working directory of running process
  - Use `D:/a/psign_$projectsha` if running on windows and directory creation successed
  - Use `/Users/runner/work/p_$projectsha` if running on windows and directory creation successed
  - Use JDK system api to create temp directory
