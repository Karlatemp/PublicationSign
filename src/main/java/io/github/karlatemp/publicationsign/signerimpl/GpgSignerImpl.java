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

    private static File createCiTmp() throws Exception {
        File tmp;
        String customTmp = System.getProperty("publication-sign.workingDir");
        if (customTmp != null) {
            tmp = new File(customTmp);
            if (!tmp.mkdirs()) {
                throw new IllegalStateException("Failed to create " + customTmp + " (JvmProp publication-sign.workingDir)");
            }
            return tmp;
        }
        customTmp = System.getenv("PUBLICATION_SIGN_WORKING_DIR");
        if (customTmp != null) {
            tmp = new File(customTmp);
            if (!tmp.mkdirs()) {
                throw new IllegalStateException("Failed to create " + customTmp + " (SysEnv PUBLICATION_SIGN_WORKING_DIR)");
            }
            return tmp;
        }
        if (!CIEnvDetect.isCI()) return null;
        tmp = new File("/tmp/publication-sign-ci");
        if (tmp.mkdirs()) return tmp;

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // GitHub WindowsServer
            tmp = new File("D:/a/psign");
            if (tmp.mkdirs()) return tmp;
        }

        tmp = File.createTempFile("psign", null);
        tmp.delete();
        if (tmp.mkdirs()) return tmp;
        throw new IllegalStateException("Failed to create a temp directory for ci");
    }

    public GpgSignerImpl(GpgSignerWorkflow workflow) {
        this.workflow = workflow;
    }

    public static File getDefaultWorkdir(Project project) {
        return new File(project.getBuildDir(), "gpg-sign");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public synchronized void initialize(Project project) throws Exception {
        File workingDir = workflow.workingDir;
        {
            File tmp = createCiTmp();
            if (tmp != null) {
                workingDir = tmp;
            }
        }
        if (workingDir == null) {
            workingDir = getDefaultWorkdir(project);
        }
        workflow.workingDir = workingDir;
        workingDir.mkdirs();
        if (!workingDir.isDirectory()) {
            throw new IllegalStateException(
                    "Working dir <" + workingDir + "> not a directory. Please specify by publication-sign.workingDir / PUBLICATION_SIGN_WORKING_DIR"
            );
        }
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
