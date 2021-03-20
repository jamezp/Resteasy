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

package org.jboss.resteasy.client.authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.client.common.PriorityServiceLoader;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Provider
public class HttpAuthenticationFeature implements Feature {
    public static final String USERNAME_KEY = "org.jboss.resteasy.client.authentication.username";
    public static final String PASSWORD_KEY = "org.jboss.resteasy.client.authentication.password";
    private final Credentials credentials;
    private final List<AuthenticationProcessor> processors;

    public HttpAuthenticationFeature() {
        credentials = null;
        processors = null;
    }

    HttpAuthenticationFeature(final Credentials credentials) {
        this.credentials = credentials;
        processors = null;
    }

    HttpAuthenticationFeature(final AuthenticationProcessor... processors) {
        this.credentials = null;
        this.processors = Arrays.asList(processors);
    }

    @Override
    public boolean configure(final FeatureContext context) {
        Credentials credentials = resolveCredentials(context);
        if (credentials != null) {
            context.register((ClientRequestFilter) requestContext -> requestContext.setProperty(CredentialAuthenticationProcessor.CREDENTIALS_KEY, credentials));
        }
        final List<AuthenticationProcessor> processors = resolveProcessors();
        if (processors.isEmpty()) {
            return false;
        }
        context.register(new HttpAuthorizationFilter(processors));
        return true;
    }

    private Credentials resolveCredentials(final FeatureContext context) {
        if (credentials != null) {
            return credentials;
        }
        String username = String.valueOf(context.getConfiguration().getProperty(USERNAME_KEY));
        String password = String.valueOf(context.getConfiguration().getProperty(PASSWORD_KEY));
        if (username == null) {
            username = System.getProperty(USERNAME_KEY);
        }
        if (password == null) {
            password = System.getProperty(PASSWORD_KEY);
        }
        if (username == null || password == null) {
            return null;
        }
        return Credentials.of(username, password);
    }

    private List<AuthenticationProcessor> resolveProcessors() {
        if (this.processors == null || this.processors.isEmpty()) {
            final List<AuthenticationProcessor> processors = new ArrayList<>();
            PriorityServiceLoader.load(AuthenticationProcessor.class).forEach(processors::add);
            return processors;
        }
        return processors;
    }
}
