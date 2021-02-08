/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/SignedMavenArtifact.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign.impl;

import io.github.karlatemp.publicationsign.signer.ArtifactSigner;
import org.gradle.api.internal.tasks.TaskDependencyInternal;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.internal.artifact.AbstractMavenArtifact;

import java.io.File;

public class SignedMavenArtifact extends AbstractMavenArtifact {
    private final MavenArtifact delegate;
    private final TaskDependencyInternal tasks;
    private final ArtifactSigner.SignResult result;

    public SignedMavenArtifact(
            MavenArtifact delegate,
            TaskDependencyInternal task,
            ArtifactSigner.SignResult result
    ) {
        this.delegate = delegate;
        this.tasks = task;
        this.result = result;
    }

    public ArtifactSigner.SignResult getResult() {
        return result;
    }

    public MavenArtifact getDelegate() {
        return delegate;
    }

    @Override
    public File getFile() {
        return result.getSignFile();
    }

    @Override
    protected String getDefaultExtension() {
        return delegate.getExtension() + "." + result.getSignExtension();
    }

    @Override
    protected String getDefaultClassifier() {
        return delegate.getClassifier();
    }

    @Override
    protected TaskDependencyInternal getDefaultBuildDependencies() {
        return tasks;
    }

    @Override
    public boolean shouldBePublished() {
        return true;
    }
}
