/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/GpgSignerImpl.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign.signerimpl;

import io.github.karlatemp.publicationsign.GpgSignerWorkflow;
import io.github.karlatemp.publicationsign.signer.AbstractArtifactSigner;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GpgSignerImpl extends AbstractArtifactSigner {
    private final GpgSignerWorkflow workflow;

    public GpgSignerImpl(GpgSignerWorkflow workflow) {
        this.workflow = workflow;
    }

    public static File getDefaultWorkdir(Project project) {
        if (System.getenv("WORK_IN_CI") != null) {
            return new File("/tmp/ci", project.getName()).getAbsoluteFile();
        }
        return new File(project.getBuildDir(), "gpg-sign");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public synchronized void initialize(Project project) throws Exception {
        File workingDir = workflow.workingDir;
        if (CIEnvDetect.isCI()) { // Force override
            workingDir = new File("/tmp/publication-sign-ci");
        }
        if (workingDir == null) {
            workingDir = getDefaultWorkdir(project);
        }
        workflow.workingDir = workingDir;
        workingDir.mkdirs();
        String homedir = workflow.homedir;
        if (CIEnvDetect.isCI()) { // Force override
            homedir = "homedir";
        }
        workflow.homedir = homedir;
        if (homedir == null) {
            return; // disabled sandbox
        }
        File homedirFile = new File(workingDir, homedir);
        Logger logger = project.getLogger();
        File pubringkbx = new File(homedirFile, "pubring.kbx");
        if (pubringkbx.isFile()) {
            return;
        }
        homedirFile.mkdirs();
        // "--batch", "--import"
        Collection<File> keys = workflow.keys;
        if (keys == null || keys.isEmpty()) {
            logger.error("[GPG Signer] Workflow no any keys. Please setup keys for sign");
            throw new RuntimeException("No keys");
        }
        for (File key : keys) {
            if (key != null) {
                processGPG(logger, "--batch", "--import", key.toString());
            }
        }
    }

    public void processGPG(Logger logger, String... cmd) throws Exception {
        processGPG(logger, false, cmd);
    }

    public void processGPG(Logger logger, boolean noHomedir, String... cmd) throws Exception {
        List<String> cmd0 = new ArrayList<>();
        cmd0.add(workflow.gpgCommandBinary);
        if (!noHomedir) {
            if (workflow.homedir != null) {
                cmd0.add("--homedir");
                cmd0.add(workflow.homedir);
            }
        }

        List<String> additionArguments = workflow.additionArguments;
        if (additionArguments != null) {
            cmd0.addAll(additionArguments);
        }

        cmd0.addAll(Arrays.asList(cmd));
        if (logger != null && logger.isInfoEnabled()) {
            logger.info("Processing `" + String.join(" ", cmd0) + "`");
        }
        File out = new File(workflow.workingDir, "gpg-" + System.currentTimeMillis() + "-" + UUID.randomUUID() + ".log");
        out.createNewFile();

        int response = new ProcessBuilder()
                .directory(workflow.workingDir)
                .command(cmd0)
                .redirectError(out)
                .redirectOutput(out)
                .start()
                .waitFor();
        if ((logger != null && logger.isInfoEnabled()) || response != 0) {
            Thread.sleep(1000L);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(out), StandardCharsets.UTF_8))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) break;
                    if (logger == null) {
                        System.out.println(line);
                    } else {
                        if (response != 0) {
                            logger.error(line);
                        } else {
                            logger.info(line);
                        }
                    }
                }
            }
        }
        if (response != 0) {
            throw new RuntimeException("GPG command response " + response + " != 0, '" + String.join(" ", cmd0) + "'");
        }
        out.delete();
    }

    @Override
    protected String getSignFileExtension(File artifactFile) {
        return "asc";
    }

    @Override
    protected void sign0(Logger logger, File artifactFile, File signFile) throws Exception {
        if (workflow.isUserMode) {
            processGPG(logger,
                    "-a", "--no-tty",
                    "--detach-sig", "--sign", artifactFile.toString()
            );
        } else {
            processGPG(logger,
                    "-a", "--no-tty", "--batch",
                    "--detach-sig", "--sign", artifactFile.toString()
            );
        }

        if (workflow.skipVerify) return;

        processGPG(logger, "--no-tty", "--verify", signFile.toString(), artifactFile.toString());
    }

}
