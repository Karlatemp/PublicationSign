/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/PublicationSignPlugin.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign;

import io.github.karlatemp.publicationsign.impl.ArtifactSignTask;
import io.github.karlatemp.publicationsign.impl.MPMavenPublication;
import io.github.karlatemp.publicationsign.impl.MPObjectFactory;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.DefaultPolymorphicNamedEntityInstantiator;
import org.gradle.api.internal.PolymorphicDomainObjectContainerInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.publish.Publication;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings({"unchecked", "rawtypes"})
public class PublicationSignPlugin implements Plugin<Project> {
    private final Field factoriesField;
    private final Field objectFactoryField;

    private static <T> T cast(Object any) {
        return (T) any;
    }

    public PublicationSignPlugin() throws Throwable {
        Field factories = DefaultPolymorphicNamedEntityInstantiator.class.getDeclaredField("factories");
        factories.setAccessible(true);
        this.factoriesField = factories;

        Field objectFactory = MavenPublishPlugin.class.getDeclaredField("objectFactory");
        objectFactory.setAccessible(true);
        this.objectFactoryField = objectFactory;
    }

    static class PostInit implements Consumer<Object> {
        private final Project project;

        PostInit(Project project) {
            this.project = project;
        }

        @Override
        public void accept(Object o) {
            if (o instanceof MPMavenPublication) {
                MPMavenPublication p = (MPMavenPublication) o;
                p.project = project;
                ArtifactSignTask.setup(project, p);
            }
        }
    }

    @Override
    public void apply(@NotNull Project project) {
        Logger logger = project.getLogger();
        logger.info("[PublicationSign] Setting up....");
        project.getExtensions()
                .create(PublicationSignExtension.NAME, PublicationSignExtension.class)
                .declaredProject = project;

        project.getPluginManager().withPlugin("maven-publish", $0000 -> project.getExtensions().configure(PublishingExtension.class, extension -> {
            PolymorphicDomainObjectContainerInternal<Publication> publications =
                    (PolymorphicDomainObjectContainerInternal<Publication>) extension.getPublications();


            DefaultPolymorphicNamedEntityInstantiator<Publication> instantiator = cast(publications.getEntityInstantiator());
            Map<Class<? extends Publication>, NamedDomainObjectFactory<? extends Publication>> factory;
            try {
                factory = cast(factoriesField.get(instantiator));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            NamedDomainObjectFactory<? extends Publication> mFactory = factory.get(MavenPublication.class);
            if (logger.isInfoEnabled()) {
                logger.info("[PublicationSign] Old MavenPublicationFactory is " + mFactory);
            }

            Class<? extends NamedDomainObjectFactory> factoryClass = mFactory.getClass();
            Field this0 = null;
            for (Field field : factoryClass.getDeclaredFields()) {
                if (field.getType() == MavenPublishPlugin.class) {
                    this0 = field;
                    this0.setAccessible(true);
                    break;
                }
            }
            if (this0 == null) {
                throw new RuntimeException("Cannot found MavenPublishPlugin instance field from " + mFactory.getClass());
            }

            MavenPublishPlugin publishPlugin;
            try {
                publishPlugin = cast(this0.get(mFactory));
                if (logger.isInfoEnabled()) {
                    logger.info("[PublicationSign] MavenPublishPlugin found: " + publishPlugin);
                }

                ObjectFactory objectFactory = cast(objectFactoryField.get(publishPlugin));
                objectFactoryField.set(publishPlugin, new MPObjectFactory(objectFactory, new PostInit(project)));
                if (logger.isInfoEnabled()) {
                    logger.info("[PublicationSign] MavenPublishPlugin injected...");
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
