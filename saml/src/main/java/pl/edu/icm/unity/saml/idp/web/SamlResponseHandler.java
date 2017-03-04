/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;

import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import com.vaadin.server.Page;
import com.vaadin.server.SynchronizedRequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

import eu.unicore.samly2.exceptions.SAMLServerException;

/**
 * Code used by various components to produce and initialize sending of SAML response.
 * 
 * @author K. Benedyczak
 */
public class SamlResponseHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlResponseHandler.class);
	protected FreemarkerHandler freemarkerHandler;
	protected AuthnResponseProcessor samlProcessor;
	
	public SamlResponseHandler(FreemarkerHandler freemarkerHandler,
			AuthnResponseProcessor samlProcessor)
	{
		this.freemarkerHandler = freemarkerHandler;
		this.samlProcessor = samlProcessor;
	}

	public void handleException(Exception e, boolean destroySession) throws EopException
	{
		log.debug("Exception raised and will trigger SAML error response from IdP", e);
		SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
		ResponseDocument respDoc = samlProcessor.getErrorResponse(convertedException);
		returnSamlErrorResponse(respDoc, convertedException, destroySession);
		throw new EopException();
	}
	
	public void returnSamlErrorResponse(ResponseDocument respDoc, SAMLServerException error, boolean destroySession)
	{
		VaadinSession.getCurrent().setAttribute(SessionDisposal.class, 
				new SessionDisposal(error, destroySession));
		VaadinSession.getCurrent().setAttribute(SAMLServerException.class, error);
		returnSamlResponse(respDoc);
	}
	
	public void returnSamlResponse(ResponseDocument respDoc)
	{
		VaadinSession.getCurrent().setAttribute(ResponseDocument.class, respDoc);
		VaadinSession.getCurrent().addRequestHandler(new SendResponseRequestHandler());
		Page.getCurrent().reload();		
	}
	
	/**
	 * This handler intercept all messages and checks if there is a SAML response in the session.
	 * If it is present then the appropriate Freemarker page is rendered which redirects the user's browser 
	 * back to the requesting SP.
	 * @author K. Benedyczak
	 */
	public class SendResponseRequestHandler extends SynchronizedRequestHandler
	{
		@Override
		public boolean synchronizedHandleRequest(VaadinSession session, VaadinRequest request, 
				VaadinResponse response) throws IOException
		{
			ResponseDocument samlResponse = session.getAttribute(ResponseDocument.class);
			if (samlResponse == null)
				return false;
			String assertion = samlResponse.xmlText();
			String encodedAssertion = Base64.encode(assertion.getBytes(StandardCharsets.UTF_8));
			SessionDisposal error = session.getAttribute(SessionDisposal.class);
			
			SAMLAuthnContext samlCtx = SAMLContextSupport.getContext();
			String serviceUrl = samlCtx.getRequestDocument().getAuthnRequest().getAssertionConsumerServiceURL();
			if (serviceUrl == null)
				serviceUrl = samlCtx.getSamlConfiguration().getReturnAddressForRequester(
						samlCtx.getRequest().getIssuer());
			Map<String, String> data = new HashMap<String, String>();
			data.put("SAMLResponse", encodedAssertion);
			data.put("samlService", serviceUrl);
			if (error != null)
				data.put("error", error.getE().getMessage());
			if (samlCtx.getRelayState() != null)
				data.put("RelayState", samlCtx.getRelayState());

			if (log.isTraceEnabled())
			{
				log.trace("About to send SAML response to " + serviceUrl + 
						", unencoded form:\n" + assertion);
				if (error != null)
					log.trace("Error information: " + error.getE().getMessage());
				if (samlCtx.getRelayState() != null)
					log.trace("RelayState: " + samlCtx.getRelayState());
			}

			SAMLContextSupport.cleanContext();
			if (error!= null && error.isDestroySession())
				session.getSession().invalidate();
			response.setContentType("application/xhtml+xml; charset=utf-8");
			PrintWriter writer = response.getWriter();
			freemarkerHandler.process("finishSaml.ftl", data, writer);
			return true;
		}
	}
	
	private static class SessionDisposal
	{
		private SAMLServerException e;
		private boolean destroySession;
		
		public SessionDisposal(SAMLServerException e, boolean destroySession)
		{
			this.e = e;
			this.destroySession = destroySession;
		}

		protected SAMLServerException getE()
		{
			return e;
		}

		protected boolean isDestroySession()
		{
			return destroySession;
		}
	}
}
