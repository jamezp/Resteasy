package org.jboss.resteasy.test.client.resource;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

public class ConfigurationInheritanceTestFilter3 implements ClientRequestFilter {
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
    }
}
