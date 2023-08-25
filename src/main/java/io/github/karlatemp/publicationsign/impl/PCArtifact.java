/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/PCArtifact.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign.impl;

import org.gradle.api.internal.tasks.DefaultTaskDependencyFactory;
import org.gradle.api.internal.tasks.TaskDependencyInternal;
import org.gradle.api.publish.maven.internal.artifact.AbstractMavenArtifact;

import java.io.File;

public class PCArtifact extends AbstractMavenArtifact implements Patch_g7_2_MavenArtifact {
    final AbstractMavenArtifact delegate;
    private final File signFile;
    private final String signExt;
    private final TaskDependencyInternal task;

    public PCArtifact(
            AbstractMavenArtifact delegate,
            File signFile,
            String signExt,
            TaskDependencyInternal task
    ) {
        super(DefaultTaskDependencyFactory.withNoAssociatedProject());

        this.delegate = delegate;
        this.signFile = signFile;
        this.signExt = signExt;
        this.task = task;
    }

    @Override
    public File getFile() {
        return signFile;
    }

    @Override
    protected String getDefaultExtension() {
        return delegate.getExtension() + "." + signExt;
    }

    @Override
    protected String getDefaultClassifier() {
        return delegate.getClassifier();
    }

    @Override
    public TaskDependencyInternal getDefaultBuildDependencies() {
        return task;
    }

    @Override
    public boolean shouldBePublished() {
        return delegate.shouldBePublished();
    }
}
