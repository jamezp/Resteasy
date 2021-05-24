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

package org.jboss.resteasy.setup;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.resteasy.utils.TimeoutUtil;
import org.junit.Assert;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SnapshotServerSetupTask implements ServerSetupTask {

    private static final int TIMEOUT = 60 * 1000; // TODO (jrp) make this configurable
    private final Map<String, AutoCloseable> snapshots = new HashMap<>();

    @Override
    public final void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        snapshots.put(containerId, takeSnapshot(managementClient));
        doSetup(managementClient, containerId);
    }

    @Override
    public final void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        final AutoCloseable snapshot = snapshots.remove(containerId);
        if (snapshot != null) {
            snapshot.close();
        }
        nonManagementCleanUp();
    }

    protected void doSetup(final ManagementClient client, final String containerId) throws Exception {
    }

    protected void nonManagementCleanUp() throws Exception {
    }

    /**
     * Takes a snapshot of the current state of the server.
     *
     * Returns a AutoCloseable that can be used to restore the server state
     *
     * @param client The client
     *
     * @return A closeable that can be used to restore the server
     */
    private static AutoCloseable takeSnapshot(ManagementClient client) {
        try {
            final ModelNode op = Operations.createOperation("take-snapshot");
            final ModelNode result = client.getControllerClient().execute(op);
            if (!Operations.isSuccessfulOutcome(result)) {
                Assert.fail("Reload operation didn't finish successfully: " + Operations.getFailureDescription(result).asString());
            }
            final String snapshot = Operations.readResult(result).asString();
            final String fileName = snapshot.contains(File.separator) ? snapshot.substring(snapshot.lastIndexOf(File.separator) + 1) : snapshot;
            return () -> {
                executeReloadAndWaitForCompletion(client.getControllerClient(), fileName);

                final ModelNode result1 = client.getControllerClient().execute(Operations.createOperation("write-config"));
                if (!Operations.isSuccessfulOutcome(result1)) {
                    Assert.fail("Failed to write config after restoring from snapshot " + Operations.getFailureDescription(result1).asString());
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to take snapshot", e);
        }
    }

    private static void executeReloadAndWaitForCompletion(final ModelControllerClient client, final String serverConfig) {
        final ModelNode op = Operations.createOperation("reload");
        if (serverConfig != null) {
            op.get("server-config").set(serverConfig);
        }
        try {
            final ModelNode result = client.execute(op);
            if (!Operations.isSuccessfulOutcome(result)) {
                Assert.fail("Reload operation didn't finish successfully: " + Operations.getFailureDescription(result).asString());
            }
        } catch (IOException e) {
            final Throwable cause = e.getCause();
            if (!(cause instanceof ExecutionException) && !(cause instanceof CancellationException)) {
                throw new UncheckedIOException(e);
            }
        }
        int adjustedTimeout = TimeoutUtil.adjust(TIMEOUT);
        long start = System.currentTimeMillis();
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("server-state");
        while (System.currentTimeMillis() - start < adjustedTimeout) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ignore) {
            }
            try {
                final ModelControllerClient liveClient = ModelControllerClient.Factory.create(
                        TestUtil.getManagementHost(), TestUtil.getManagementPort());
                try {
                    ModelNode result = liveClient.execute(operation);
                    if ("running".equalsIgnoreCase(Operations.readResult(result).asString())) {
                        return;
                    }
                } catch (IOException e) {
                    // ignore
                } finally {
                    if (liveClient != null) try {
                        liveClient.close();
                    } catch (IOException ignore) {
                    }
                }
            } catch (UnknownHostException e) {
                throw new UncheckedIOException(e);
            }
        }
        Assert.fail("Live Server did not reload in the imparted time of " +
                adjustedTimeout + "(" + TIMEOUT + ") milliseconds");
    }
}
