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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) maybe there's a better representation instead of username/password
class Credentials {
    private final String username;
    // TODO (jrp) should we just use a string?
    private final byte[] password;


    Credentials(final String username, final byte[] password) {
        this.username = username;
        this.password = Arrays.copyOf(password, password.length);
    }

    String getUsername() {
        return username;
    }

    byte[] getPassword() {
        return password;
    }

    static Credentials of(final String username, final String password) {
        return new Credentials(username, password.getBytes(StandardCharsets.UTF_8));
    }
}
