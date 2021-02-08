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
import org.gradle.api.Project;
import org.gradle.api.internal.tasks.AbstractTaskDependency;
import org.gradle.api.internal.tasks.TaskDependencyInternal;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.logging.Logger;
import org.gradle.api.publish.internal.DefaultPublicationArtifactSet;
import org.gradle.api.publish.internal.PublicationArtifactSet;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.internal.artifact.AbstractMavenArtifact;
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

@SuppressWarnings("unchecked")
public class ArtifactSignTask extends DefaultTask {
    static final Method artifactsToBePublishedMethod;

    static {
        try {
            Method artifactsToBePublished =
                    DefaultMavenPublication.class.getDeclaredMethod("artifactsToBePublished");
            artifactsToBePublished.setAccessible(true);
            artifactsToBePublishedMethod = artifactsToBePublished;
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final DefaultMavenPublication publication;
    private DefaultPublicationArtifactSet<SignedMavenArtifact> signedArtifacts;
    private PublicationArtifactSet<? extends MavenArtifact> publicationArtifactSet;

    @Override
    public @NotNull TaskDependency getMustRunAfter() {
        return new AbstractTaskDependency() {
            @Override
            public void visitDependencies(@NotNull TaskDependencyResolveContext context) {
                try {
                    @SuppressWarnings("unchecked")
                    Set<MavenArtifact> artifacts = (Set<MavenArtifact>)
                            artifactsToBePublishedMethod.invoke(publication);

                    for (MavenArtifact artifact : artifacts) {
                        if (artifact instanceof AbstractMavenArtifact) {
                            if (!((AbstractMavenArtifact) artifact).shouldBePublished()) {
                                continue;
                            }
                        }
                        context.add(artifact.getBuildDependencies());
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Inject
    public ArtifactSignTask(DefaultMavenPublication publication) {
        this.publication = publication;
        dependsOn(getMustRunAfter());
    }

    @TaskAction
    protected void invoke() throws Exception {
        Logger logger = getLogger();
        if (logger.isInfoEnabled()) {
            logger.info("Signing artifacts for " + publication.getName());
        }
        PublicationSignExtension signExtension = getProject().getExtensions().getByType(PublicationSignExtension.class);
        ArtifactSigner signer = signExtension.newGpgSigner(getProject());
        if (signer == null) {
            logger.error("GPG Signer not found. Skip");
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Using gpg signer: " + signer);
        }
        signer.initialize(getProject());

        if (signedArtifacts == null) {
            throw new RuntimeException("Internal error: signedArtifacts not setup.");
        }
        if (publicationArtifactSet == null) {
            throw new RuntimeException("Internal error: publicationArtifactSet not setup");
        }

        for (MavenArtifact artifact : publicationArtifactSet) {

            File artifactFile = artifact.getFile();
            if (logger.isInfoEnabled()) {
                logger.info("Signing " + artifactFile);
            }

            ArtifactSigner.SignResult result = signer.doSign(logger, artifactFile);
            if (result == null) continue;
            SignedMavenArtifact signedMavenArtifact = new SignedMavenArtifact(
                    artifact,
                    TaskDependencyInternal.EMPTY,
                    result
            );
            signedArtifacts.add(signedMavenArtifact);
            if (logger.isDebugEnabled()) {
                logger.debug("Added signed artifact " + signedMavenArtifact);
            }
        }
    }

    public static void setup(Project project, DefaultMavenPublication publication) {
        TaskProvider<ArtifactSignTask> provider = project.getTasks().register(
                "signArtifactsFor" + Capitalize.capitalize(publication.getName()) + "Publication",
                ArtifactSignTask.class, publication
        );
        provider.configure(task -> {
            task.setGroup("publishing");
        });
        if (publication instanceof MPMavenPublication) {
            ((MPMavenPublication) publication).completeSignArtifactTask(provider.get());
        }
    }


    public void hookArtifacts(
            DefaultPublicationArtifactSet<SignedMavenArtifact> signedArtifacts,
            PublicationArtifactSet<?> publicationArtifactSet
    ) {
        this.signedArtifacts = signedArtifacts;
        this.publicationArtifactSet = (PublicationArtifactSet<? extends MavenArtifact>) publicationArtifactSet;
    }
}
