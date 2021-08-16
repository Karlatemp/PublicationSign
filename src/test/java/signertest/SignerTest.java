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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class SignerTest {
    @Test
    public void runTest() throws Exception {
        File wkDir = new File("testing/tv1");
        failOnFailed(GradleRunner.create()
                .withProjectDir(wkDir)
                .withArguments("--info", "publishToMavenLocal", "--full-stacktrace")
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

    @Test
    public void runTestWithNoSigner() throws Exception {
        File wkDir = new File("testing/tv2");
        failOnFailed(GradleRunner.create()
                .withProjectDir(wkDir)
                .withArguments("--info", "publishToMavenLocal", "--full-stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build()
        );
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
