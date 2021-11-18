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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SignerTest {
    private static final File TESTING = new File("testing");
    private static final Pattern VALID_NAME = Pattern.compile("[^A-Za-z_]");

    private static String toValidName(String name) {
        return VALID_NAME.matcher(name).replaceAll("_");
    }

    private static void copyTemplate(File template, File dir, String name) throws Exception {
        dir.mkdirs();
        build_gradle:
        {
            File build_gradle_template = new File(template, "build.gradle");
            File build_gradle_target = new File(dir, "build.gradle");
            String build_gradle_template_text = FilesKt.readText(build_gradle_template, UTF_8);

            if (build_gradle_target.isFile()) {
                String targetContent = FilesKt.readText(build_gradle_target, UTF_8);
                if (targetContent.equals(build_gradle_template_text)) break build_gradle;
            }

            FilesKt.writeText(build_gradle_target, build_gradle_template_text, UTF_8);
        }

        settings_gradle:
        {
            File settings_gradle_template = new File(template, "settings.gradle");
            File settings_gradle_target = new File(dir, "settings.gradle");
            String build_gradle_template_text = FilesKt.readText(settings_gradle_template, UTF_8) + "\n" +
                    "rootProject.name = '" + toValidName(name) + "'\n";

            if (settings_gradle_target.isFile()) {
                String targetContent = FilesKt.readText(settings_gradle_target, UTF_8);
                if (targetContent.equals(build_gradle_template_text)) break settings_gradle;
            }

            FilesKt.writeText(settings_gradle_target, build_gradle_template_text, UTF_8);
        }
    }

    private static class TestRunner {
        String name;
        String fname;
        Function<GradleRunner, GradleRunner> func;

        static TestRunner of(String name, Function<GradleRunner, GradleRunner> func) {
            TestRunner tr = new TestRunner();
            tr.func = func;
            tr.name = name;
            tr.fname = toValidName(name);
            return tr;
        }

        public static Function<GradleRunner, GradleRunner> embedded() {
            return Function.identity();
        }

        public static Function<GradleRunner, GradleRunner> gradleVersion(String ver) {
            return runner -> runner.withGradleDistribution(
                    URI.create("https://services.gradle.org/distributions/gradle-" + ver + "-all.zip")
            );
        }
    }

    private static class TestUnit {
        String name;

        interface TF2C {
            void runTest(TestRunner runner, File workDir) throws Exception;
        }

        TF2C action;

        static TestUnit of(String name, TF2C action) {
            TestUnit unit = new TestUnit();
            unit.action = action;
            unit.name = name;
            return unit;
        }
    }

    @TestFactory
    public Collection<DynamicNode> tests() {
        List<DynamicNode> tests = new ArrayList<>();

        TestUnit[] testUnits = {
                TestUnit.of("normal-test", this::runTest),
                TestUnit.of("without-signer", this::runTestWithNoSigner),
        };

        TestRunner[] runners = {
                TestRunner.of("embedded", TestRunner.embedded()),
                TestRunner.of("gradle 7.2", TestRunner.gradleVersion("7.2")),
        };

        for (TestRunner runner : runners) {
            List<DynamicNode> runnerTests = new ArrayList<>();
            for (TestUnit testUnit : testUnits) {
                File workDir = new File(TESTING, "r-" + runner.name + "-" + testUnit.name);
                File template = new File(TESTING, "template-" + testUnit.name);
                String testName = testUnit.name;// + " [" + runner.name + "]";
                if (!template.isDirectory()) {
                    runnerTests.add(DynamicTest.dynamicTest(testName, thrownError(new FileNotFoundException(template.getAbsolutePath()))));
                    continue;
                }
                try {
                    copyTemplate(template, workDir, testUnit.name + "-" + runner.name);
                } catch (Exception e) {
                    runnerTests.add(DynamicTest.dynamicTest(testName, thrownError(new FileNotFoundException(template.getAbsolutePath()))));
                    continue;
                }
                runnerTests.add(DynamicTest.dynamicTest(
                        testName,
                        () -> testUnit.action.runTest(runner, workDir))
                );
            }
            tests.add(DynamicContainer.dynamicContainer(
                    runner.name, runnerTests
            ));
        }

        return tests;
    }

    private Executable thrownError(Exception e) {
        return () -> {
            throw e;
        };
    }

    public void runTest(TestRunner runner, File workDir) throws Exception {
        failOnFailed(runner.func.apply(GradleRunner.create())
                .withProjectDir(workDir)
                .withArguments("--info", "publishToMavenLocal", "--full-stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build()
        );

        // re-run
        BuildResult rerun = runner.func.apply(GradleRunner.create())
                .withProjectDir(workDir)
                .withArguments("--info", "signAllPublications", "--full-stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();
        failOnFailed(rerun);
        Assertions.assertTrue(rerun.getOutput().contains("Task :signPublicationMain UP-TO-DATE"));
    }

    public void runTestWithNoSigner(TestRunner runner, File workDir) throws Exception {
        failOnFailed(runner.func.apply(GradleRunner.create())
                .withProjectDir(workDir)
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
