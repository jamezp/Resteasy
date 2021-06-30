package org.jboss.resteasy.test.microprofile.restclient.resource;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.io.Closeable;

public interface HealthService extends Closeable {
    @GET
    @Produces({ MediaType.APPLICATION_JSON})
    @Path("/health")
    HealthCheckData getHealthData()         throws WebApplicationException;
}