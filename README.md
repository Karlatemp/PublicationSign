# Publication Sign

A gradle plugin for sign publication artifacts. For fast deploy to maven central

System requirement: `gpg` installed

## Configuration

### Plugin apply

```groovy
plugins {
    // Load this plugin first for resolve magic problems
    id 'io.github.karlatemp.publication-sign' version '1.3.8'
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
