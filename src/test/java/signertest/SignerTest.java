/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.test/SignerTest.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package signertest;

import kotlin.io.FilesKt;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SignerTest {
    @Test
    public void runTest() throws Exception {
        File wkDir = new File("testing/tv1");
        wkDir.mkdirs();
        FilesKt.writeText(
                new File(wkDir, "settings.gradle"),
                "",
                StandardCharsets.UTF_8
        );
        FilesKt.writeText(
                new File(wkDir, "build.gradle"),
                "plugins {\n" +
                        "  id 'maven-publish'\n" +
                        "  id 'io.github.karlatemp.publication-sign'\n" +
                        "  id 'java'\n" +
                        "}\n\n" +
                        "publishing {\n" +
                        "    publications { container ->\n" +
                        "        register(\"main\", MavenPublication.class) { publication ->\n" +
                        "            publication.from(project.components.java)\n" +
                        "        }\n" +
                        "   }\n" +
                        "}\n\n" +
                        "publicationSign {\n" +
                        "    setupWorkflow { workflow ->\n" +
                        "        workflow.fastSetup(\"../testing-keys/keys.pub\", \"../testing-keys/keys.pri\")\n" +
                        "    }\n" +
                        "}\n",
                StandardCharsets.UTF_8
        );
        failOnFailed(GradleRunner.create()
                .withProjectDir(wkDir)
                .withArguments("--info", "signAllPublications", "--full-stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build()
        );

        // re-run
        BuildResult rerun = GradleRunner.create()
                .withProjectDir(wkDir)
                .withArguments("--info", "signAllPublications", "--full-stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();
        failOnFailed(rerun);
        Assertions.assertTrue(rerun.getOutput().contains("Task :signPublicationMain UP-TO-DATE"));
    }

    private static void failOnFailed(BuildResult signAllPublications) {
        List<BuildTask> tasks = signAllPublications.getTasks();
        if (tasks.isEmpty()) {
            throw new RuntimeException("No task executed");
        }
        for (BuildTask task : tasks) {
            if (task.getOutcome() == TaskOutcome.FAILED) {
                throw new RuntimeException("Failed");
            }
        }
    }
}
