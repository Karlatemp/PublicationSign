/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/ArtifactSigner.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign.signer;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.io.File;

public interface ArtifactSigner {
    SignResult getSignFile(File file);

    interface SignResult {
        File getArtifactFile();

        File getSignFile();

        String getSignExtension();
    }

    SignResult doSign(Logger logger, File artifactFile) throws Exception;

    default void initialize(Project project) throws Exception {
    }

}
