package org.jboss.resteasy.client.jaxrs.internal.proxy.processors.invocation;

import org.jboss.resteasy.client.jaxrs.i18n.Messages;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FormParamProcessor extends AbstractInvocationCollectionProcessor
{

   public FormParamProcessor(final String paramName)
   {
      super(paramName);
   }

   @Override
   protected ClientInvocation apply(ClientInvocation target, Object object)
   {
      return apply(target, new Object[]{object});
   }

   @Override
   protected ClientInvocation apply(ClientInvocation target, Object[] objects)
   {
      for (Object object : objects) {
         Form form = null;
         Object entity = target.getEntity();
         if (entity != null)
         {
            if (entity instanceof Form)
            {
               form = (Form) entity;
            }
            else
            {
               throw new RuntimeException(Messages.MESSAGES.cannotSetFormParameter());
            }
         }
         else
         {
            form = new Form();
            target.setEntity(Entity.form(form));
         }
         String value = target.getClientConfiguration().toString(object);
         form.param(paramName, value);
      }
      return target;
   }

}
