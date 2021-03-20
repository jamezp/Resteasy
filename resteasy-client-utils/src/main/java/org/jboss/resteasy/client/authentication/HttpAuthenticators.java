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

import java.util.Objects;

import org.jboss.resteasy.client.logging.ClientMessages;

/**
 * A utility to define the feature to place on a REST client or on a request.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("unused")
public class HttpAuthenticators {

    /**
     * Creates a feature which will handle only BASIC authentication.
     *
     * @param username the user name for authentication
     * @param password the password for authentication
     *
     * @return the feature to add to your client builder or request builder
     */
    public static HttpAuthenticationFeature basic(final String username, final String password) {
        final Credentials credentials = Credentials.of(
                Objects.requireNonNull(username, ClientMessages.MESSAGES.requiredValue("username")),
                Objects.requireNonNull(password, ClientMessages.MESSAGES.requiredValue("password"))
        );
        return new HttpAuthenticationFeature(new BasicAuthenticationProcessor(credentials));
    }

    /**
     * Creates a feature which will handle only DIGEST authentication.
     *
     * @param username the user name for authentication
     * @param password the password for authentication
     *
     * @return the feature to add to your client builder or request builder
     */
    public static HttpAuthenticationFeature digest(final String username, final String password) {
        final Credentials credentials = Credentials.of(
                Objects.requireNonNull(username, ClientMessages.MESSAGES.requiredValue("username")),
                Objects.requireNonNull(password, ClientMessages.MESSAGES.requiredValue("password"))
        );
        return new HttpAuthenticationFeature(new DigestAuthenticationProcessor(credentials));
    }

    /**
     * Uses a service loader to discover which {@linkplain AuthenticationProcessor processors} to use.
     * <p>
     * For BASIC or DIGEST which requires a user name and password you can set the
     * {@code org.jboss.resteasy.client.authentication.username} and {@code org.jboss.resteasy.client.authentication.password}
     * context parameters to use the build-in processors.
     * </p>
     *
     * @return the feature to add to your client builder or request builder
     */
    public static HttpAuthenticationFeature discover() {
        return new HttpAuthenticationFeature();
    }

    /**
     * Uses a service loader to discover which {@linkplain AuthenticationProcessor processors} to use.
     *
     * @param username the user name to use for authentication
     * @param password the password to use for authentication
     *
     * @return the feature to add to your client builder or request builder
     */
    public static HttpAuthenticationFeature discover(final String username, final String password) {
        final Credentials credentials = Credentials.of(
                Objects.requireNonNull(username, ClientMessages.MESSAGES.requiredValue("username")),
                Objects.requireNonNull(password, ClientMessages.MESSAGES.requiredValue("password"))
        );
        return new HttpAuthenticationFeature(credentials);
    }
}
