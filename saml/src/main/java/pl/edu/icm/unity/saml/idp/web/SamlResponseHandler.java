/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import eu.unicore.samly2.exceptions.SAMLServerException;
import io.imunity.vaadin.auth.server.ProxyAuthenticationFilter;
import io.imunity.vaadin.endpoint.common.EopException;
import org.apache.logging.log4j.Logger;

import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.VaadinContextSession;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter.SamlIdpStatisticReporterFactory;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Code used by various components to produce and initialize sending of SAML
 * response.
 * 
 * @author K. Benedyczak
 */
public class SamlResponseHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlResponseHandler.class);
	protected FreemarkerAppHandler freemarkerHandler;
	protected AuthnResponseProcessor samlProcessor;
	private final SamlIdpStatisticReporter reporter;

	public SamlResponseHandler(FreemarkerAppHandler freemarkerHandler,
			AuthnResponseProcessor samlProcessor, 
			SamlIdpStatisticReporterFactory reporterFactory,
			Endpoint endpoint
			)
	{
		this.freemarkerHandler = freemarkerHandler;
		this.samlProcessor = samlProcessor;
		this.reporter = reporterFactory.getForEndpoint(endpoint);
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
		VaadinSession session = VaadinSession.getCurrent();
		session.getSession().setAttribute(SessionDisposal.class.getName(), new SessionDisposal(error, destroySession));
		session.getSession().setAttribute(SAMLServerException.class.getName(), error); // TODO: is this needed?
		returnSamlResponse(respDoc, Status.FAILED);
	}

	public void returnSamlResponse(ResponseDocument respDoc, Status status)
	{
		VaadinSession session = VaadinSession.getCurrent();
		session.getSession().setAttribute(ResponseDocument.class.getName(), respDoc);
		session.addRequestHandler(new SendResponseRequestHandler());
		reporter.reportStatus(SamlSessionService.getVaadinContext(), status);
		UI.getCurrent().getPage().reload();
	}

	/**
	 * This handler intercept all messages and checks if there is a SAML response in
	 * the session. If it is present then the appropriate Freemarker page is
	 * rendered which redirects the user's browser back to the requesting SP.
	 */
	public class SendResponseRequestHandler extends SynchronizedRequestHandler
	{
		@Override
		public boolean synchronizedHandleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response)
				throws IOException
		{
			ResponseDocument samlResponse = (ResponseDocument)session.getSession().getAttribute(ResponseDocument.class.getName());
			if (samlResponse == null)
				return false;
			String assertion = samlResponse.xmlText();
			String encodedAssertion = Base64.getEncoder().encodeToString(assertion.getBytes(StandardCharsets.UTF_8));
			SessionDisposal error = (SessionDisposal)session.getSession().getAttribute(SessionDisposal.class.getName());

			VaadinContextSession signInContextSession = new VaadinContextSession(session.getSession());
			SAMLAuthnContext samlCtx = SamlSessionService.getVaadinContext(signInContextSession);
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
				log.trace("About to send SAML response to " + serviceUrl + ", unencoded form:\n" + assertion);
				if (error != null)
					log.trace("Error information: " + error.getE().getMessage());
				if (samlCtx.getRelayState() != null)
					log.trace("RelayState: " + samlCtx.getRelayState());
			}

			SamlSessionService.cleanContext(signInContextSession);

			if (error != null && error.isDestroySession())
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
