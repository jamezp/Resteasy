package org.jboss.resteasy.test.microprofile.restclient.resource;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class Ignore404ExceptionMapper implements ResponseExceptionMapper<WebApplicationException> {

    @Override
    public WebApplicationException toThrowable(Response response) {
        return null;
    }

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        // as per MP-Health specification
        return 404 == status;
    }
}