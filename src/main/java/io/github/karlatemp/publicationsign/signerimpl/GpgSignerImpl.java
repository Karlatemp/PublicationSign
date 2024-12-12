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

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.*;

import static java.nio.file.attribute.PosixFilePermission.*;

public class GpgSignerImpl extends AbstractArtifactSigner {
    private final GpgSignerWorkflow workflow;

    static final FileAttribute<Set<PosixFilePermission>> dirPermissions =
            PosixFilePermissions.asFileAttribute(EnumSet
                    .of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));
    private static final boolean isPosix =
            FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    private static boolean mkdir1(File dir) {
        if (dir.isDirectory()) return true;
        if (dir.isFile()) return false;
        try {
            Files.createDirectory(dir.toPath(), isPosix ? new FileAttribute[]{dirPermissions} : new FileAttribute[0]);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static File createCiTmp(Project project, Logger logger) throws Exception {
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
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(project.getProjectDir().getAbsolutePath().getBytes(StandardCharsets.UTF_8));
        String project_sha1 = String.format("%040x", new BigInteger(1, digest.digest()));

        tmp = new File("/tmp/ps_" + project_sha1).getAbsoluteFile();
        if (mkdir1(tmp)) return tmp;

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("windows")) {
            // GitHub WindowsServer
            tmp = new File("D:/a/psign_" + project_sha1);
            if (mkdir1(tmp)) return tmp;

        }
        if (os.contains("mac")) {
            tmp = new File("/Users/runner/work/p_" + project_sha1);
            if (mkdir1(tmp)) return tmp;
        }

        tmp = Files.createTempDirectory("psign").toFile();
        if (true) return tmp;
        if (tmp.isDirectory()) return tmp;
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
        Logger logger = project.getLogger();
        File workingDir = workflow.workingDir;
        {
            File tmp = createCiTmp(project, logger);
            if (tmp != null) {
                workingDir = tmp;
            }
        }
        if (workingDir == null) {
            workingDir = getDefaultWorkdir(project);
        }
        workflow.workingDir = workingDir;
        mkdir1(workingDir);
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
        File pubringkbx = new File(homedirFile, "pubring.kbx");
        if (pubringkbx.isFile()) {
            return;
        }
        mkdir1(homedirFile);
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
        File out = new File(workflow.workingDir, "gpg-" + System.currentTimeMillis() + "-" + UUID.randomUUID() + ".log");
        out.createNewFile();
        execCommand(logger, workflow.workingDir, out, cmd0);
    }

    private static void execCommand(Logger logger, File workingDir, File out, List<String> cmd0) throws Exception {
        if (logger != null && logger.isInfoEnabled()) {
            logger.info("Processing `" + String.join(" ", cmd0) + "`" + (workingDir == null ? "" : " in " + workingDir));
        }
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command(cmd0);
        if (workingDir != null) {
            processBuilder.directory(workingDir);
        }
        if (out == null) {
            out = File.createTempFile("tmp-psrun", ".log");
        }
        int response = processBuilder
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
