/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 *
 * PublicationSign/PublicationSign.main/MPObjectFactory.java
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
 */

package io.github.karlatemp.publicationsign.impl;

import org.gradle.api.*;
import org.gradle.api.file.*;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication;
import org.gradle.api.reflect.ObjectInstantiationException;

import java.util.function.Consumer;

@SuppressWarnings({"NullableProblems", "UnstableApiUsage"})
public class MPObjectFactory implements ObjectFactory {
    private final ObjectFactory delegate;
    private final Consumer<Object> postNewInstance;

    public MPObjectFactory(ObjectFactory delegate, Consumer<Object> postNewInstance) {
        this.delegate = delegate;
        this.postNewInstance = postNewInstance;
    }

    protected <T> T post(T instance) {
        if (postNewInstance != null)
            postNewInstance.accept(instance);
        return instance;
    }

    @Override
    public <T> T newInstance(Class<? extends T> type, Object... parameters) throws ObjectInstantiationException {
        if (type == DefaultMavenPublication.class) {
            return post(type.cast(delegate.newInstance(MPMavenPublication.class, parameters)));
        }
        return post(delegate.newInstance(type, parameters));
    }

    @Override
    public <T extends Named> T named(Class<T> type, String name) throws ObjectInstantiationException {
        return delegate.named(type, name);
    }

    @Override
    @Incubating
    public SourceDirectorySet sourceDirectorySet(String name, String displayName) {
        return delegate.sourceDirectorySet(name, displayName);
    }

    @Override
    @Incubating
    public ConfigurableFileCollection fileCollection() {
        return delegate.fileCollection();
    }

    @Override
    @Incubating
    public ConfigurableFileTree fileTree() {
        return delegate.fileTree();
    }

    @Override
    @Incubating
    public <T> NamedDomainObjectContainer<T> domainObjectContainer(Class<T> elementType) {
        return delegate.domainObjectContainer(elementType);
    }

    @Override
    @Incubating
    public <T> NamedDomainObjectContainer<T> domainObjectContainer(Class<T> elementType, NamedDomainObjectFactory<T> factory) {
        return delegate.domainObjectContainer(elementType, factory);
    }

    @Override
    @Incubating
    public <T> ExtensiblePolymorphicDomainObjectContainer<T> polymorphicDomainObjectContainer(Class<T> elementType) {
        return delegate.polymorphicDomainObjectContainer(elementType);
    }

    @Override
    @Incubating
    public <T> DomainObjectSet<T> domainObjectSet(Class<T> elementType) {
        return delegate.domainObjectSet(elementType);
    }

    @Override
    @Incubating
    public <T> NamedDomainObjectSet<T> namedDomainObjectSet(Class<T> elementType) {
        return delegate.namedDomainObjectSet(elementType);
    }

    @Override
    @Incubating
    public <T> NamedDomainObjectList<T> namedDomainObjectList(Class<T> elementType) {
        return delegate.namedDomainObjectList(elementType);
    }

    @Override
    public <T> Property<T> property(Class<T> valueType) {
        return delegate.property(valueType);
    }

    @Override
    public <T> ListProperty<T> listProperty(Class<T> elementType) {
        return delegate.listProperty(elementType);
    }

    @Override
    public <T> SetProperty<T> setProperty(Class<T> elementType) {
        return delegate.setProperty(elementType);
    }

    @Override
    @Incubating
    public <K, V> MapProperty<K, V> mapProperty(Class<K> keyType, Class<V> valueType) {
        return delegate.mapProperty(keyType, valueType);
    }

    @Override
    @Incubating
    public DirectoryProperty directoryProperty() {
        return delegate.directoryProperty();
    }

    @Override
    @Incubating
    public RegularFileProperty fileProperty() {
        return delegate.fileProperty();
    }
}
