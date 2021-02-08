/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/MPMavenPublication.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign.impl;

import groovy.lang.Lazy;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.artifacts.dsl.dependencies.PlatformSupport;
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectDependencyPublicationResolver;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.api.internal.file.collections.MinimalFileSet;
import org.gradle.api.internal.tasks.AbstractTaskDependency;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.publish.internal.CompositePublicationArtifactSet;
import org.gradle.api.publish.internal.DefaultPublicationArtifactSet;
import org.gradle.api.publish.internal.PublicationArtifactSet;
import org.gradle.api.publish.internal.versionmapping.VersionMappingStrategyInternal;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication;
import org.gradle.api.publish.maven.internal.publisher.MavenNormalizedPublication;
import org.gradle.api.publish.maven.internal.publisher.MutableMavenProjectIdentity;
import org.gradle.internal.Cast;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.typeconversion.NotationParser;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class MPMavenPublication extends DefaultMavenPublication {

    private final CompositePublicationArtifactSet<MavenArtifact> publishableArtifacts0;
    private final DefaultPublicationArtifactSet<SignedMavenArtifact> signedArtifacts;
    @Lazy
    public Project project;
    private ArtifactSignTask artifactSignTask;
    private Runnable onGpgSignTaskComplete;

    public void completeSignArtifactTask(ArtifactSignTask artifactSignTask) {
        this.artifactSignTask = artifactSignTask;
        onGpgSignTaskComplete.run();
        onGpgSignTaskComplete = null;
    }

    @Inject
    public MPMavenPublication(
            String name,
            MutableMavenProjectIdentity projectIdentity,
            NotationParser<Object, MavenArtifact> mavenArtifactParser,
            Instantiator instantiator,
            ObjectFactory objectFactory,
            ProjectDependencyPublicationResolver projectDependencyResolver,
            FileCollectionFactory fileCollectionFactory,
            ImmutableAttributesFactory immutableAttributesFactory,
            CollectionCallbackActionDecorator collectionCallbackActionDecorator,
            VersionMappingStrategyInternal versionMappingStrategy,
            PlatformSupport platformSupport
    ) throws Exception {
        super(name, projectIdentity, mavenArtifactParser, instantiator, objectFactory, projectDependencyResolver, fileCollectionFactory, immutableAttributesFactory, collectionCallbackActionDecorator, versionMappingStrategy, platformSupport);

        Field publishableArtifactsField = DefaultMavenPublication.class.getDeclaredField("publishableArtifacts");
        publishableArtifactsField.setAccessible(true);
        PublicationArtifactSet<?> publicationArtifactSet = (PublicationArtifactSet<?>) publishableArtifactsField.get(this);

        FileCollectionInternal files000 = fileCollectionFactory.create(new AbstractTaskDependency() {
            @Override
            public void visitDependencies(@NotNull TaskDependencyResolveContext context) {
                context.add(artifactSignTask);
            }
        }, new MinimalFileSet() {
            @Override
            public @NotNull Set<File> getFiles() {
                return Collections.emptySet();
            }

            @Override
            public @NotNull String getDisplayName() {
                return "Signed Artifacts";
            }
        });
        DefaultPublicationArtifactSet<SignedMavenArtifact> signedArtifacts = new DefaultPublicationArtifactSet<SignedMavenArtifact>(
                SignedMavenArtifact.class, "signed artifacts for " + name, fileCollectionFactory, collectionCallbackActionDecorator
        ) {
            @Override
            public FileCollection getFiles() {
                return files000;
            }
        };
        this.publishableArtifacts0 = new CompositePublicationArtifactSet<>(MavenArtifact.class, Cast.uncheckedCast(new PublicationArtifactSet<?>[]{
                publicationArtifactSet,
                signedArtifacts
        }));
        this.signedArtifacts = signedArtifacts;

        //noinspection CodeBlock2Expr
        onGpgSignTaskComplete = () -> {
            artifactSignTask.hookArtifacts(signedArtifacts, publicationArtifactSet);
        };
    }

    @Override
    public MavenNormalizedPublication asNormalisedPublication() {
        MavenNormalizedPublication parent = super.asNormalisedPublication();

        return new MavenNormalizedPublication(
                getName(),
                getMavenProjectIdentity(),
                parent.getPackaging(),
                parent.getPomArtifact(),
                parent.getMainArtifact(),
                plusSigned(parent.getAllArtifacts(), signedArtifacts)
        );
    }

    private static Set<MavenArtifact> plusSigned(Set<MavenArtifact> old, Set<SignedMavenArtifact> signedArtifacts) {
        LinkedHashSet<MavenArtifact> result = new LinkedHashSet<>(old);

        for (MavenArtifact artifact : old) {
            for (SignedMavenArtifact signedMavenArtifact : signedArtifacts) {
                MavenArtifact delegate = signedMavenArtifact.getDelegate();
                if (Objects.equals(delegate.getClassifier(), artifact.getClassifier())) {
                    if (Objects.equals(delegate.getExtension(), (artifact.getExtension()))) {
                        result.add(signedMavenArtifact);
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public PublicationArtifactSet<MavenArtifact> getPublishableArtifacts() {
        super.getPublishableArtifacts();
        return publishableArtifacts0;
    }
}
