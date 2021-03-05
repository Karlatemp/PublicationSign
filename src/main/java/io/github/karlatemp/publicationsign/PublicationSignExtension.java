/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/PublicationSignExtension.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign;

import groovy.lang.Closure;
import io.github.karlatemp.publicationsign.signer.ArtifactSigner;
import io.github.karlatemp.publicationsign.signerimpl.GpgSignerImpl;
import org.gradle.api.Action;
import org.gradle.api.Project;

import java.util.function.Function;

public class PublicationSignExtension {
    public static final String NAME = "publicationSign";
    public Project declaredProject;

    public ArtifactSigner customSigner;

    public interface GpgSignerAllocator {
        public ArtifactSigner newGpgSigner(Project project) throws Exception;
    }

    public GpgSignerAllocator signerAllocator;

    public void signerAllocator(Function<Project, ArtifactSigner> allocator) {
        signerAllocator = allocator::apply;
    }

    public void signerAllocator(Closure<ArtifactSigner> closure) {
        signerAllocator = closure::call;
    }


    public ArtifactSigner newGpgSigner(Project current, Project project) throws Exception {
        if (signerAllocator != null)
            return signerAllocator.newGpgSigner(project);

        Project parent = current.getParent();
        if (parent == null) return null;

        PublicationSignExtension extensionParent;
        try {
            extensionParent = parent.getExtensions().getByType(PublicationSignExtension.class);
        } catch (Exception ignored) {
            return null;
        }
        return extensionParent.newGpgSigner(parent, project);
    }

    public ArtifactSigner newGpgSigner(Project project) throws Exception {
        return newGpgSigner(project, project);
    }

    public void setupWorkflow(Action<? super GpgSignerWorkflow> configure) {
        GpgSignerWorkflow workflow = new GpgSignerWorkflow();
        workflow.declaredProject = declaredProject;
        workflow.workingDir = GpgSignerImpl.getDefaultWorkdir(declaredProject);
        configure.execute(workflow);
        GpgSignerImpl signer = new GpgSignerImpl(workflow);
        signerAllocator = $ -> signer;
    }

}
