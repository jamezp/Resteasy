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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Strings {

    private static final char[] HEX_TABLE = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    public static String bytesToHexString(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(HEX_TABLE[b >> 4 & 0x0f]).append(HEX_TABLE[b & 0x0f]);
        }
        return builder.toString();
    }

    public static String md5(final String... tokens) throws IOException {
        final StringBuilder sb = new StringBuilder(256);
        for (final String token : tokens) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(token);
        }
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException ex) {
            throw new IOException(ex.getMessage());
        }
        md.update(sb.toString().getBytes(StandardCharsets.UTF_8), 0, sb.length());
        final byte[] md5hash = md.digest();
        return bytesToHexString(md5hash);
    }
}
