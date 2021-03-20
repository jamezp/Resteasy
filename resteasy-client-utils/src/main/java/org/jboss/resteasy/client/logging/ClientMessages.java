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

package org.jboss.resteasy.client.logging;

import java.util.function.Supplier;
import javax.ws.rs.WebApplicationException;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "RESTEASYCLIENT")
public interface ClientMessages {

    ClientMessages MESSAGES = Messages.getBundle(ClientMessages.class);

    @Message(id = 1, value = "Failed to resolve credentials for %s authentication")
        // TODO (jrp) is this the correct exception to throw? We likely want to continue on
    WebApplicationException notAuthorized(String authType);

    @Message(id = 2, value = "A value for %s is required.")
    Supplier<String> requiredValue(String name);
}
