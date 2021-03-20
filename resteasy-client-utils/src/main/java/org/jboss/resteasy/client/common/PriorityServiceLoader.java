/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.client.common;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) how is performance?
public class PriorityServiceLoader<S extends Priority> implements Iterable<S> {
    private final List<S> services;

    private PriorityServiceLoader(final List<S> services) {
        this.services = services;
    }

    public static <S extends Priority> PriorityServiceLoader<S> load(final Class<S> service) {
        return load(service, getClassLoader());
    }

    public static <S extends Priority> PriorityServiceLoader<S> load(final Class<S> service, final ClassLoader cl) {
        final ServiceLoader<S> loader = ServiceLoader.load(service, cl);
        final List<S> services = new ArrayList<>();
        loader.forEach(services::add);
        services.sort(Comparator.comparingInt(Priority::priority));
        return new PriorityServiceLoader<>(services);
    }

    @Override
    public Iterator<S> iterator() {
        final Iterator<S> delegate = services.iterator();
        return new Iterator<S>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public S next() {
                return delegate.next();
            }
        };
    }

    private static ClassLoader getClassLoader() {
        if (System.getSecurityManager() == null) {
            ClassLoader result = Thread.currentThread().getContextClassLoader();
            if (result == null) {
                result = PriorityServiceLoader.class.getClassLoader();
            }
            return result;
        }
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
            ClassLoader result = Thread.currentThread().getContextClassLoader();
            if (result == null) {
                result = PriorityServiceLoader.class.getClassLoader();
            }
            return result;
        });
    }
}
