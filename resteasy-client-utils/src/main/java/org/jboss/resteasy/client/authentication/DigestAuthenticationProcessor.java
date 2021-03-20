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

import static org.jboss.resteasy.client.common.Strings.bytesToHexString;
import static org.jboss.resteasy.client.common.Strings.md5;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.client.ClientRequestContext;

import org.jboss.resteasy.client.logging.ClientMessages;
import org.kohsuke.MetaInfServices;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MetaInfServices(AuthenticationProcessor.class)
@SuppressWarnings("unused")
public class DigestAuthenticationProcessor extends CredentialAuthenticationProcessor implements AuthenticationProcessor {
    // TODO (jrp) verify this pattern
    private static final Pattern KEY_VALUE_PAIR_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*(\"([^\"]+)\"|(\\w+))\\s*,?\\s*");

    private final SecureRandom generator;

    public DigestAuthenticationProcessor() {
        this(null);
    }

    protected DigestAuthenticationProcessor(final Credentials credentials) {
        super(credentials);
        generator = new SecureRandom();
    }

    @Override
    public String createRequestHeader(final ClientRequestContext requestContext, final List<String> authenticateHeader) throws IOException {
        final DigestScheme digestScheme = DigestScheme.of(authenticateHeader);
        if (digestScheme == null) {
            // TODO (jrp) what do we do?
            return "";
        }
        return createHeaderValue(digestScheme, requestContext);
    }

    @Override
    public boolean canProcess(final ClientRequestContext requestContext, final AuthenticationType type) {
        return resolveCredentials(requestContext).isPresent() && type == AuthenticationType.DIGEST;
    }

    @Override
    public int priority() {
        return 500;
    }

    private String createHeaderValue(final DigestScheme scheme, final ClientRequestContext requestContext) throws IOException {
        final Credentials credentials = resolveCredentials(requestContext).orElseThrow(() -> ClientMessages.MESSAGES.notAuthorized("digest"));
        final StringBuilder result = new StringBuilder(100);
        result.append("Digest ");
        append(result, "username", credentials.getUsername());
        append(result, "realm", scheme.realm);
        append(result, "qop", scheme.qop, false);
        append(result, "nonce", scheme.nonce);
        append(result, "opaque", scheme.opaque);
        // TODO (jrp) what do we do if this is not an MD5 version?
        append(result, "algorithm", scheme.algorithm, false);

        final String uri = fullRelativeUri(requestContext.getUri());
        append(result, "uri", uri);

        final String ha1;
        if ("MD5-sess".equalsIgnoreCase(scheme.algorithm)) {
            ha1 = md5(md5(credentials.getUsername(), scheme.realm,
                    new String(credentials.getPassword(), getCharset(requestContext.getMediaType()))));
        } else {
            ha1 = md5(credentials.getUsername(), scheme.realm,
                    new String(credentials.getPassword(), getCharset(requestContext.getMediaType())));
        }

        final String ha2 = md5(requestContext.getMethod(), uri);

        final String response;
        if (scheme.qop == null) {
            response = md5(ha1, scheme.nonce, ha2);
        } else {
            final byte[] bytes = new byte[8];
            generator.nextBytes(bytes);
            final String cnonce = bytesToHexString(bytes);
            append(result, "cnonce", cnonce);
            final String nc = String.format("%08x", scheme.nc.incrementAndGet());
            append(result, "nc", nc, false);
            response = md5(ha1, scheme.nonce, nc, cnonce, scheme.qop, ha2);
        }
        append(result, "response", response);

        return result.toString();
    }

    private static void append(final StringBuilder sb, final String key, final String value) {
        append(sb, key, value, true);
    }

    private static void append(final StringBuilder sb, final String key, final String value, final boolean quoted) {
        if (value != null) {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                sb.append(',');
            }
            sb.append(key)
                    .append('=');
            if (quoted) {
                sb.append('"');
            }
            sb.append(value);
            if (quoted) {
                sb.append('"');
            }
        }
    }

    private static String fullRelativeUri(final URI uri) {
        if (uri == null) {
            return null;
        }
        final String query = uri.getRawQuery();
        return uri.getRawPath() + (query != null && query.length() > 0 ? "?" + query : "");
    }

    // https://tools.ietf.org/html/rfc2617#section-3.2.2
    static class DigestScheme {

        private final String realm;
        private final String nonce;
        private final String opaque;
        private final String algorithm;
        private final String qop;
        // TODO (jrp) decide what we should do with a stale=true entry
        private final boolean stale;
        private final AtomicInteger nc;

        DigestScheme(final String realm, final String qop, final String nonce, final String opaque, final String algorithm,
                     final boolean stale) {
            this.realm = realm;
            this.nonce = nonce;
            this.opaque = opaque;
            this.qop = qop;
            this.algorithm = algorithm;
            this.stale = stale;
            this.nc = new AtomicInteger();
        }

        static DigestScheme of(final List<?> headerValues) {
            if (headerValues == null) {
                return null;
            }

            for (final Object lineObject : headerValues) {

                if (!(lineObject instanceof String)) {
                    continue;
                }
                final String line = (String) lineObject;
                final String[] parts = line.trim().split("\\s+", 2);

                if (parts.length != 2) {
                    continue;
                }
                if (!"digest".equalsIgnoreCase(parts[0])) {
                    continue;
                }

                String realm = null;
                String nonce = null;
                String opaque = null;
                String qop = null;
                String algorithm = null;
                boolean stale = false;

                final Matcher match = KEY_VALUE_PAIR_PATTERN.matcher(parts[1]);
                while (match.find()) {
                    final int groupCount = match.groupCount();
                    if (groupCount != 4) {
                        continue;
                    }
                    final String key = match.group(1);
                    final String quotedVal = match.group(4);
                    final String val = quotedVal == null ? match.group(3) : quotedVal;
                    if ("qop".equals(key)) {
                        qop = val;
                    } else if ("realm".equals(key)) {
                        realm = val;
                    } else if ("nonce".equals(key)) {
                        nonce = val;
                    } else if ("opaque".equals(key)) {
                        opaque = val;
                    } else if ("stale".equals(key)) {
                        stale = Boolean.parseBoolean(val);
                    } else if ("algorithm".equals(key)) {
                        algorithm = val;
                    }
                }
                return new DigestScheme(realm, qop, nonce, opaque, algorithm, stale);
            }
            return null;
        }
    }
}
