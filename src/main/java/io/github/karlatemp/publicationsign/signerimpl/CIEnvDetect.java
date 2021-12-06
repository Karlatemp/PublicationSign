/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/CIEnvDelect.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign.signerimpl;

@SuppressWarnings("RedundantIfStatement")
public class CIEnvDetect {
    public static boolean isCI() {
        if (Boolean.parseBoolean(System.getenv("NOT_IN_CI"))) {
            return false;
        }
        if (Boolean.parseBoolean(System.getenv("CI")))
            return true;
        if (System.getenv("GITHUB_RUN_ID") != null) return true;
        return false;
    }
}
