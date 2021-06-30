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
import io.github.karlatemp.publicationsign.impl.Capitalize;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.jetbrains.annotations.NotNull;

public class PublicationSignPlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project project) {
        Logger logger = project.getLogger();
        logger.info("[PublicationSign] Setting up....");
        project.getExtensions()
                .create(PublicationSignExtension.NAME, PublicationSignExtension.class)
                .declaredProject = project;

        project.getPluginManager().withPlugin("maven-publish", $0000 -> project.getExtensions().configure(PublishingExtension.class, extension -> {
            Task signAllPublications = project.getTasks().create("signAllPublications");
            signAllPublications.setGroup("publishing");

            extension.getPublications().all(publication -> {
                if (publication instanceof MavenPublication) {
                    String taskName = "signPublication" + Capitalize.capitalize(publication.getName());
                    Task subSignTask = project.getTasks().create(taskName, ArtifactSignTask.class, publication);
                    subSignTask.setGroup("publishing");
                    signAllPublications.dependsOn(subSignTask);
                }
            });
        }));
    }
}
