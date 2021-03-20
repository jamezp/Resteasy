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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Provider
@SuppressWarnings("unused")
public class HttpAuthorizationFilter implements ClientResponseFilter {
    private static final Logger LOGGER = Logger.getLogger(HttpAuthorizationFilter.class);
    private static final String PROCESSED_KEY = "org.jboss.resteasy.client.authentication.processed";
    private final Collection<AuthenticationProcessor> processors;

    public HttpAuthorizationFilter(final AuthenticationProcessor... processors) {
        this.processors = Arrays.asList(processors);
    }

    public HttpAuthorizationFilter(final Iterator<AuthenticationProcessor> processors) {
        this.processors = new ArrayList<>();
        processors.forEachRemaining(this.processors::add);
    }

    public HttpAuthorizationFilter(final Collection<AuthenticationProcessor> processors) {
        this.processors = new ArrayList<>(processors);
    }

    @Override
    public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext) throws IOException {

        if (requestContext.getProperty(PROCESSED_KEY) != null) {
            return;
        }

        AuthenticationType type = null;
        if (responseContext.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
            final String authString = responseContext.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE);
            if (authString != null) {
                final String upperCaseAuth = authString.trim().toUpperCase(Locale.ROOT).trim();
                // Split the string
                final int index = upperCaseAuth.indexOf(' ');
                if (index > 0) {
                    type = AuthenticationType.resolve(upperCaseAuth.substring(0, index));
                } else if (index == -1) {
                    type = AuthenticationType.resolve(upperCaseAuth);
                }
            }
        }
        for (AuthenticationProcessor processor : processors) {
            if (processor.canProcess(requestContext, type)) {
                if (repeatRequest(requestContext, responseContext,
                        processor.createRequestHeader(requestContext, responseContext.getHeaders().get(HttpHeaders.WWW_AUTHENTICATE)))) {
                    break;
                } else {
                    LOGGER.debugf("Failed to process request for type %s with processor %s", type, processor.getClass().getCanonicalName());
                }
            }
        }

        // TODO (jrp) what do we do if we can't process the type?
    }

    private static boolean repeatRequest(final ClientRequestContext request, final ClientResponseContext response, final String authHeader) {
        if (authHeader == null) {
            return false;
        }
        final Client client = request.getClient();
        final String method = request.getMethod();
        final MediaType mediaType = request.getMediaType();

        final Invocation.Builder builder = client.target(request.getUri()).request(mediaType);
        final MultivaluedMap<String, Object> newHeaders = new MultivaluedHashMap<>();

        for (Map.Entry<String, List<Object>> entry : request.getHeaders().entrySet()) {
            if (HttpHeaders.AUTHORIZATION.equals(entry.getKey())) {
                continue;
            }
            newHeaders.put(entry.getKey(), entry.getValue());
        }

        newHeaders.add(HttpHeaders.AUTHORIZATION, authHeader);
        builder.headers(newHeaders);
        builder.property(PROCESSED_KEY, true);

        final Invocation invocation;
        if (request.getEntity() == null) {
            invocation = builder.build(method);
        } else {
            invocation = builder.build(method,
                    Entity.entity(request.getEntity(), request.getMediaType()));
        }
        final Response newResponse = invocation.invoke();

        if (newResponse.hasEntity()) {
            response.setEntityStream(newResponse.readEntity(InputStream.class));
        }
        final MultivaluedMap<String, String> headers = response.getHeaders();
        headers.clear();
        headers.putAll(newResponse.getStringHeaders());
        response.setStatus(newResponse.getStatus());
        return response.getStatus() != Response.Status.UNAUTHORIZED.getStatusCode();
    }
}
