package org.jboss.resteasy.plugins.providers.atom;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import org.jboss.resteasy.core.messagebody.AsyncBufferedMessageBodyWriter;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBContextFinder;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBMarshalException;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBUnmarshalException;
import org.jboss.resteasy.plugins.providers.resteasy_atom.i18n.Messages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashSet;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Provider
@Produces("application/atom+*")
@Consumes("application/atom+*")
public class AtomFeedProvider implements MessageBodyReader<Feed>, AsyncBufferedMessageBodyWriter<Feed>
{
   @Context
   protected Providers providers;

   protected JAXBContextFinder getFinder(MediaType type)
   {
      ContextResolver<JAXBContextFinder> resolver = providers.getContextResolver(JAXBContextFinder.class, type);
      if (resolver == null) return null;
      return resolver.getContext(null);
   }

   public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return Feed.class.isAssignableFrom(type);
   }

   public Feed readFrom(Class<Feed> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
   {
      LogMessages.LOGGER.debugf("Provider : %s,  Method : readFrom", getClass().getName());
      JAXBContextFinder finder = getFinder(mediaType);
      if (finder == null)
      {
         throw new JAXBUnmarshalException(Messages.MESSAGES.unableToFindJAXBContext(mediaType));
      }

      try
      {
         JAXBContext ctx = finder.findCachedContext(Feed.class, mediaType, annotations);
         Feed feed = (Feed) ctx.createUnmarshaller().unmarshal(entityStream);
         for (Entry entry : feed.getEntries())
         {
            entry.setFinder(finder);
            if (entry.getContent() != null) entry.getContent().setFinder(finder);
         }
         return feed;
      }
      catch (JAXBException e)
      {
         throw new JAXBUnmarshalException(Messages.MESSAGES.unableToUnmarshal(mediaType), e);
      }
   }

   public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return Feed.class.isAssignableFrom(type);
   }

   public long getSize(Feed feed, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return -1;
   }

   public void writeTo(Feed feed, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
   {
      LogMessages.LOGGER.debugf("Provider : %s,  Method : writeTo", getClass().getName());
      JAXBContextFinder finder = getFinder(mediaType);
      if (finder == null)
      {
         throw new JAXBUnmarshalException(Messages.MESSAGES.unableToFindJAXBContext(mediaType));
      }
      HashSet<Class> set = new HashSet<Class>();
      set.add(Feed.class);
      for (Entry entry : feed.getEntries())
      {
         if (entry.getAnyOtherJAXBObject() != null)
         {
            set.add(entry.getAnyOtherJAXBObject().getClass());
         }
         if (entry.getContent() != null && entry.getContent().getJAXBObject() != null)
         {
            set.add(entry.getContent().getJAXBObject().getClass());
         }
      }
      try
      {
         JAXBContext ctx = finder.findCacheContext(mediaType, annotations, set.toArray(new Class[set.size()]));
         Marshaller marshaller = ctx.createMarshaller();

         final Object mapper = createMapper();
         try {
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", mapper);
         } catch (PropertyException e) {
            LogMessages.LOGGER.debugf(e, "Failed to set org.glassfish.jaxb.namespacePrefixMapper with %s", mapper);
         }
         try {
            marshaller.setProperty("com.sun.jaxb.namespacePrefixMapper", mapper);
         } catch (PropertyException e) {
            LogMessages.LOGGER.debugf(e, "Failed to set com.sun.jaxb.namespacePrefixMapper with %s", mapper);
         }

         marshaller.marshal(feed, entityStream);
      }
      catch (JAXBException e)
      {
         throw new JAXBMarshalException(Messages.MESSAGES.unableToMarshal(mediaType), e);
      }
   }

   private Object createMapper() {
      Class<?> type;
      try {
         type = Class.forName("org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper");
      } catch (ClassNotFoundException e) {
         try {
            type = Class.forName("com.sun.xml.bind.marshaller.NamespacePrefixMapper");
         } catch (ClassNotFoundException e2) {
            throw new RuntimeException("Failed to create a NamespacePrefixMapper. Missing Jakarta XML Binding implementation.");
         }
      }
      final ProxyFactory factory = new ProxyFactory() {
         @Override
         protected ClassLoader getClassLoader0() {
            return AtomFeedProvider.class.getClassLoader();
         }
      };
      factory.setSuperclass(type);
      factory.setFilter(m -> m.getName().equals("getPreferredPrefix"));

      final MethodHandler handler = (self, thisMethod, proceed, args) -> {
         if (args != null && args.length > 2) {
            if ("http://www.w3.org/2005/Atom".equals(args[1])) {
               return "atom";
            } else {
               return args[1];
            }
         }
         return null;
      };

      try {
         return factory.create(new Class<?>[0], new Object[0], handler);
      } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
         throw new RuntimeException("Failed to create a NamespacePrefixMapper", e);
      }
   }
}
