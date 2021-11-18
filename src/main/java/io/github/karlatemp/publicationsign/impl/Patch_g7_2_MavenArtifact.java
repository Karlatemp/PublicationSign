/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/Patch_g7_2_MavenAtrifact.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign.impl;

import org.gradle.api.tasks.TaskDependency;

interface Patch_g7_2_MavenArtifact {
    TaskDependency getDefaultBuildDependencies();
}
