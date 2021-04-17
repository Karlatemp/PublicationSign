/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/AbstractArtifactSigner.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign.signer;

import org.gradle.api.logging.Logger;

import java.io.File;

public abstract class AbstractArtifactSigner implements ArtifactSigner {
    public static class SerializableSignResult implements SignResult {
        private final File artifact;
        private final File sign;
        private final String ext;

        public SerializableSignResult(
                File artifactFile,
                File signFile,
                String signExtension
        ) {
            this.artifact = artifactFile;
            this.sign = signFile;
            this.ext = signExtension;
        }

        @Override
        public File getArtifactFile() {
            return artifact;
        }

        @Override
        public File getSignFile() {
            return sign;
        }

        @Override
        public String getSignExtension() {
            return ext;
        }

        @Override
        public String toString() {
            return "SerializableSignResult{" +
                    "artifact=" + artifact +
                    ", sign=" + sign +
                    ", ext='" + ext + '\'' +
                    '}';
        }
    }

    protected abstract String getSignFileExtension(File artifactFile);

    protected File getSignFile(File artifactFile, String signExt) {
        return new File(artifactFile.getPath() + "." + signExt);
    }

    protected abstract void sign0(Logger logger, File artifactFile, File signFile) throws Exception;

    @Override
    public SignResult getSignFile(File file) {
        String ext = getSignFileExtension(file);
        File sf = getSignFile(file, ext);
        return new SerializableSignResult(file, sf, ext);
    }

    @Override
    public SignResult doSign(Logger logger, File artifactFile) throws Exception {
        String ext = getSignFileExtension(artifactFile);
        File signFile = getSignFile(artifactFile, ext);
        if (signFile == null) return null;
        //noinspection ResultOfMethodCallIgnored
        signFile.delete(); // Delete old sign result
        sign0(logger, artifactFile, signFile);
        return new SerializableSignResult(artifactFile, signFile, ext);
    }
}
