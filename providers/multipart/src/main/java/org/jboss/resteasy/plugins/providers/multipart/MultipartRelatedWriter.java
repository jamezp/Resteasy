package org.jboss.resteasy.plugins.providers.multipart;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.AsyncMessageBodyWriter;
import org.jboss.resteasy.spi.AsyncOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CompletionStage;

/**
 * The {@link MessageBodyWriter} implementation to serialize
 * {@link MultipartRelatedOutput} objects.
 *
 * @author Attila Kiraly
 * @version $Revision: 1 $
 */
@Provider
@Produces("multipart/related")
public class MultipartRelatedWriter extends AbstractMultipartRelatedWriter
        implements AsyncMessageBodyWriter<MultipartRelatedOutput> {

    public boolean isWriteable(Class<?> type, Type genericType,
                               Annotation[] annotations, MediaType mediaType) {
        return MultipartRelatedOutput.class.isAssignableFrom(type);
    }

    public long getSize(MultipartRelatedOutput multipartRelatedOutput,
                        Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    public void writeTo(MultipartRelatedOutput multipartRelatedOutput,
                        Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException,
            WebApplicationException {
        writeRelated(multipartRelatedOutput, mediaType, httpHeaders,
                entityStream);
    }

    @Override
    public CompletionStage<Void> asyncWriteTo(MultipartRelatedOutput multipartRelatedOutput, Class<?> type, Type genericType, Annotation[] annotations,
                                              MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                                              AsyncOutputStream entityStream) {
        return asyncWriteRelated(multipartRelatedOutput, mediaType, httpHeaders,
                entityStream);
    }
}
