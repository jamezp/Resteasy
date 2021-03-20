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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class CredentialAuthenticationProcessor implements AuthenticationProcessor {
    static final String CREDENTIALS_KEY = "org.jboss.resteasy.client.authentication.credentials";
    private final Credentials credentials;

    protected CredentialAuthenticationProcessor(final Credentials credentials) {
        this.credentials = credentials;
    }

    Optional<Credentials> resolveCredentials(final ClientRequestContext requestContext) {
        if (credentials != null) {
            return Optional.of(credentials);
        }
        final Object result = requestContext.getProperty(CREDENTIALS_KEY);
        if (result instanceof Credentials) {
            return Optional.of((Credentials) result);
        }
        return Optional.empty();
    }

    static Charset getCharset(final MediaType mediaType) {
        final String charset = (mediaType == null) ? null : mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);
        return (charset == null) ? StandardCharsets.UTF_8 : Charset.forName(charset);
    }
}
