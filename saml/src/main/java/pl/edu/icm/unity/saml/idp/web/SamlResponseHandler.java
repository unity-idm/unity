/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.Page;
import com.vaadin.server.SynchronizedRequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

import eu.unicore.samly2.exceptions.SAMLServerException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.webui.LoginInProgressService.VaadinContextSession;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Code used by various components to produce and initialize sending of SAML response.
 * 
 * @author K. Benedyczak
 */
public class SamlResponseHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlResponseHandler.class);
	protected FreemarkerAppHandler freemarkerHandler;
	protected AuthnResponseProcessor samlProcessor;
	
	public SamlResponseHandler(FreemarkerAppHandler freemarkerHandler,
			AuthnResponseProcessor samlProcessor)
	{
		this.freemarkerHandler = freemarkerHandler;
		this.samlProcessor = samlProcessor;
	}

	public void handleException(Exception e, boolean destroySession) throws EopException
	{
		handleExceptionNotThrowing(e, destroySession);
		throw new EopException();
	}

	public void handleExceptionNotThrowing(Exception e, boolean destroySession)
	{
		log.warn("Exception raised and will trigger SAML error response from IdP", e);
		SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
		ResponseDocument respDoc = samlProcessor.getErrorResponse(convertedException);
		returnSamlErrorResponse(respDoc, convertedException, destroySession);
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
		VaadinSession session = VaadinSession.getCurrent();
		session.setAttribute(ResponseDocument.class, respDoc);
		session.addRequestHandler(new SendResponseRequestHandler());
		Page.getCurrent().reload();		
	}
	
	/**
	 * This handler intercept all messages and checks if there is a SAML response in the session.
	 * If it is present then the appropriate Freemarker page is rendered which redirects the user's browser 
	 * back to the requesting SP.
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
			String encodedAssertion = Base64.getEncoder().encodeToString(assertion.getBytes(StandardCharsets.UTF_8));
			SessionDisposal error = session.getAttribute(SessionDisposal.class);
			
			SAMLAuthnContext samlCtx = SamlSessionService.getVaadinContext();
			String serviceUrl = samlCtx.getResponseDestination();
			Map<String, String> data = new HashMap<>();
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

			SamlSessionService.cleanContext(new VaadinContextSession(session.getSession()));
			
			if (error!= null && error.isDestroySession())
				session.getSession().invalidate();
			else
				session.getSession().setAttribute(ProxyAuthenticationFilter.AUTOMATED_LOGIN_FIRED, null);
			
			response.setContentType("application/xhtml+xml; charset=utf-8");
			PrintWriter writer = response.getWriter();
			freemarkerHandler.printGenericPage(writer, "samlFinish.ftl", data);
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
