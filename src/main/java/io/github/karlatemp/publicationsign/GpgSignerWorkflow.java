/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/GpgSignerWorkflow.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign;

import io.github.karlatemp.publicationsign.signerimpl.GpgSignerImpl;
import org.gradle.api.Project;

import java.io.File;
import java.util.*;

public class GpgSignerWorkflow {

    /**
     * The working dir for running GPG commands.
     */
    public File workingDir;

    /**
     * The gpg command argument --homedir
     * <p>
     * No need to modify this value under normal circumstances
     */
    public String homedir = "gpg-homedir";

    /**
     * Public & Private keys
     * <p>
     * Will be imported when workflow (re-)initialize
     */
    public Collection<File> keys;

    /**
     * The binary of gpg
     */
    public String gpgCommandBinary = "gpg";

    /**
     * The addition arguments for running GPG commands.
     */
    public List<String> additionArguments;

    public void addAdditionArguments(String args) {
        if (additionArguments == null) additionArguments = new ArrayList<>();
        additionArguments.add(args);
    }

    public void addAdditionArguments(String... args) {
        if (additionArguments == null) {
            additionArguments = new ArrayList<>(Arrays.asList(args));
        } else {
            additionArguments.addAll(Arrays.asList(args));
        }
    }

    public void addAdditionArguments(List<String> args) {
        if (additionArguments == null) {
            additionArguments = new ArrayList<>(args);
        } else {
            additionArguments.addAll(args);
        }
    }

    public void addKey(File key) {
        if (keys == null) keys = new ArrayList<>();
        keys.add(key);
    }

    public void addKey(String key) {
        addKey(new File(
                Objects.requireNonNull(workingDir, "working dir not setup"),
                key
        ));
    }

    /// Fast setup
    public Project declaredProject;
    public boolean isUserMode = false;

    private void assertDeclaredProjectValid() {
        Objects.requireNonNull(
                declaredProject,
                "Fast setup methods only can be used in `publicationSign.setupWorkflow { ... }`"
        );

        if (workingDir == null)
            workingDir = GpgSignerImpl.getDefaultWorkdir(declaredProject);
        if (homedir == null) {
            homedir = "gpg-homedir";
        }
    }

    public void fastSetup(
            String publicKey, String privateKey
    ) throws Exception {
        assertDeclaredProjectValid();
        File homedir0 = new File(workingDir, homedir);
        File pubringkbx = new File(homedir0, "pubring.kbx");
        if (pubringkbx.isFile()) {
            // Key imported. skip
            return;
        }
        { // CI
            File key1 = declaredProject.file(publicKey);
            File key2 = declaredProject.file(privateKey);
            if (key1.isFile() && key2.isFile()) {
                addKey(key1);
                addKey(key2);
                return; // CI
            }
        }
        { // Import from user
            homedir = null; // disable homedir
            isUserMode = true;

            Object keyId = declaredProject.findProperty("signing.keyId");
            if (keyId != null) {
                addAdditionArguments("--local-user", String.valueOf(keyId));
            }
        }
    }

}
