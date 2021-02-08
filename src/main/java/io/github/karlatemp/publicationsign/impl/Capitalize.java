/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/Capitalize.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign.impl;

import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Function;

public class Capitalize {
    private static final Function<String, String> CAPTIALIZE;

    private static Class<?> findClass(
            ClassLoader loader,
            String... names
    ) throws ClassNotFoundException {
        ClassNotFoundException notFound = new ClassNotFoundException(String.join(", ", names));
        for (String name : names) {
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException cnfe) {
                notFound.addSuppressed(cnfe);
            }
        }
        throw notFound;
    }

    static {
        try {
            Class<?> StringUtils = findClass(
                    DefaultMavenPublication.class.getClassLoader(),
                    "org.apache.commons.lang.StringUtils",
                    "org.gradle.internal.impldep.org.apache.commons.lang.StringUtils"
            );
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.findStatic(StringUtils, "capitalize", MethodType.methodType(String.class, String.class));
            CAPTIALIZE = func -> {
                try {
                    return (String) handle.invoke(func);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            };
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static String capitalize(String string) {
        return CAPTIALIZE.apply(string);
    }
}
