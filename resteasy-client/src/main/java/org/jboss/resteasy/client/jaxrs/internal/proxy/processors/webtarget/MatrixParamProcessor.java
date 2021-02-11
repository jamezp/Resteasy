package org.jboss.resteasy.client.jaxrs.internal.proxy.processors.webtarget;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;

import javax.ws.rs.client.WebTarget;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MatrixParamProcessor extends AbstractWebTargetCollectionProcessor
{

   public MatrixParamProcessor(final String paramName)
   {
      super(paramName);
   }

   @Override
   protected ClientInvocationBuilder apply(ClientInvocationBuilder target, Object object) {
      // no-op
      return null;
   }
   @Override
   protected ClientInvocationBuilder apply(ClientInvocationBuilder target, Object[] objects) {
      // no-op
      return null;
   }

   @Override
   protected WebTarget apply(WebTarget target, Object object)
   {
      return apply(target, new Object[]{object});
   }

   @Override
   protected WebTarget apply(WebTarget target, Object[] objects)
   {
      return target.matrixParam(paramName, objects);
   }

}
