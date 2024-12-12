/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/ArtifactSignTask.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign.impl;

import io.github.karlatemp.publicationsign.PublicationSignExtension;
import io.github.karlatemp.publicationsign.signer.ArtifactSigner;
import org.gradle.api.DefaultTask;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.tasks.DefaultTaskDependency;
import org.gradle.api.internal.tasks.TaskDependencyInternal;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.logging.Logger;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.internal.artifact.AbstractMavenArtifact;
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ArtifactSignTask extends DefaultTask {
    private static Field DMP$metadataArtifacts;
    private static Field DMP$derivedArtifacts;
    private static Field DMP$mainArtifacts;
    private final DefaultMavenPublication mp;

    private static void setupFields() throws Throwable {
        if (DMP$mainArtifacts == null) {
            DMP$mainArtifacts = DefaultMavenPublication.class.getDeclaredField("mainArtifacts");
            DMP$mainArtifacts.setAccessible(true);
        }
        if (DMP$derivedArtifacts == null) {
            DMP$derivedArtifacts = DefaultMavenPublication.class.getDeclaredField("derivedArtifacts");
            DMP$derivedArtifacts.setAccessible(true);
        }
        if (DMP$metadataArtifacts == null) {
            DMP$metadataArtifacts = DefaultMavenPublication.class.getDeclaredField("metadataArtifacts");
            DMP$metadataArtifacts.setAccessible(true);
        }
    }

    private final Set<PCArtifact> signArtifacts = new HashSet<>();

    @SuppressWarnings("unchecked")
    @Inject
    public ArtifactSignTask(
            MavenPublication publication
    ) throws Throwable {
        if (!(publication instanceof DefaultMavenPublication)) {
            throw new UnsupportedOperationException(publication.getClass().toString());
        }
        setupFields();
        DefaultMavenPublication mPublication = (DefaultMavenPublication) publication;
        this.mp = mPublication;
        DomainObjectSet<MavenArtifact> derived = (DomainObjectSet<MavenArtifact>) DMP$derivedArtifacts.get(mPublication);
        TaskDependencyInternal TASK_THIS = new DefaultTaskDependency() {
            @Override
            public void visitDependencies(TaskDependencyResolveContext taskDependencyResolveContext) {
                taskDependencyResolveContext.execute(ArtifactSignTask.this);
            }

            @Override
            public @NotNull Set<? extends Task> getDependencies(@Nullable Task task) {
                return Collections.singleton(ArtifactSignTask.this);
            }
        };

        {
            DomainObjectSet<MavenArtifact> metadataArtifacts = (DomainObjectSet<MavenArtifact>) DMP$metadataArtifacts.get(mPublication);
            DomainObjectSet<MavenArtifact> mainArtifacts = (DomainObjectSet<MavenArtifact>) DMP$mainArtifacts.get(mPublication);
            if (signerUnInitialized() == null) {
                Project project = getProject();
                project.afterEvaluate($$$ -> {
                    register(mainArtifacts, derived, TASK_THIS);
                    register(metadataArtifacts, derived, TASK_THIS);
                });
            } else {
                register(mainArtifacts, derived, TASK_THIS);
                register(metadataArtifacts, derived, TASK_THIS);
            }
        }
        dependsOn(new DefaultTaskDependency() {
            @Override
            public void visitDependencies(@NotNull TaskDependencyResolveContext taskDependencyResolveContext) {
                for (PCArtifact artifact : signArtifacts) {
                    taskDependencyResolveContext.add(artifact.delegate.getBuildDependencies());
                }
            }

            @Override
            public Set<? extends Task> getDependencies(@Nullable Task task) {
                return Collections.emptySet();
            }
        });
    }

    private static String artifactToString(Project project, MavenArtifact artifact) {
        StringBuilder result = new StringBuilder();
        result.append(project.getName());
        String classifier = artifact.getClassifier();
        if (classifier != null) {
            result.append('-').append(classifier);
        }
        result.append('-').append(project.getVersion());
        String extension = artifact.getExtension();
        if (extension != null) {
            result.append('.').append(extension);
        }
        return result.toString();
    }

    private void register(DomainObjectSet<MavenArtifact> artifacts, DomainObjectSet<MavenArtifact> derived, TaskDependencyInternal task) {
        artifacts.all(artifact -> {
            try {
                ArtifactSigner artifactSigner = signerUnInitialized();
                if (artifactSigner == null) {
                    Logger logger = getLogger();
                    if (logger.isInfoEnabled()) {
                        logger.info("Failed register " + artifactToString(getProject(), artifact) + " for signing: signer not found.");
                    }
                    return;
                }
                PCArtifact sign = new PCArtifact(
                        (AbstractMavenArtifact) artifact,
                        artifactSigner.signFile(
                                getLogger(),
                                artifact.getFile()
                        ),
                        artifactSigner.getSignExt(
                                artifact.getFile()
                        ),
                        task
                );
                derived.add(sign);
                signArtifacts.add(sign);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        artifacts.whenObjectRemoved(artifact -> {
            Iterator<PCArtifact> artifactIterator = signArtifacts.iterator();
            while (artifactIterator.hasNext()) {
                PCArtifact signFile = artifactIterator.next();
                if (signFile.delegate.equals(artifact)) {
                    mp.removeDerivedArtifact(signFile);
                    artifactIterator.remove();
                }
            }
        });
    }

    protected ArtifactSigner signer() throws Exception {
        ArtifactSigner signer = signerUnInitialized();
        if (signer != null) signer.initialize(getProject());
        return signer;
    }

    protected ArtifactSigner signerUnInitialized() throws Exception {
        Logger logger = getLogger();
        PublicationSignExtension signExtension = getProject().getExtensions().getByType(PublicationSignExtension.class);
        ArtifactSigner signer = signExtension.newGpgSigner(getProject());
        if (signer == null) {
            return null;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Using gpg signer: " + signer);
        }
        return signer;
    }

    @TaskAction
    protected void execute() throws Throwable {
        ArtifactSigner signer = signer();
        Logger logger = getLogger();
        if (signer == null) {
            logger.error("GPG Signer not found. Skip");
            return;
        }

        if (signArtifacts.isEmpty()) {
            logger.warn("Nothing to sign");
            return;
        }

        for (PCArtifact artifact : signArtifacts) {

            File artifactFile = artifact.delegate.getFile();
            if (logger.isInfoEnabled()) {
                logger.info("Signing " + artifactFile);
            }

            ArtifactSigner.SignResult result = signer.doSign(logger, artifactFile);
            if (result == null) {
                if (logger.isErrorEnabled()) {
                    logger.error("Failed to sign " + artifactFile.getPath());
                }
                continue;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Added signed artifact " + result.getSignFile());
            }
        }
    }

    @InputFiles
    protected Set<File> getSources() {
        HashSet<File> src = new HashSet<>();
        for (PCArtifact artifact : signArtifacts) {
            File file = artifact.delegate.getFile();
            if (file.exists()) {
                src.add(file);
            }
        }
        return src;
    }

    @OutputFiles
    protected Set<File> getOuts() {
        HashSet<File> src = new HashSet<>();
        for (PCArtifact artifact : signArtifacts) {
            if (artifact.delegate.getFile().exists()) {
                src.add(artifact.getFile());
            }
        }
        return src;
    }
}
