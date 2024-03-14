/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import io.imunity.vaadin.endpoint.common.EopException;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthIdpStatisticReporter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Redirects the client's browser creating URL with Vaadin response (or error).
 * 
 * @author K. Benedyczak
 */
public class OAuthResponseHandler
{
	private final OAuthSessionService oauthSessionService;
	public final OAuthIdpStatisticReporter statReporter;
	private final FreemarkerAppHandler freemarkerHandler;

	public OAuthResponseHandler(OAuthSessionService oauthSessionService, OAuthIdpStatisticReporter statReporter,
			FreemarkerAppHandler freemarkerHandler)
	{
		this.oauthSessionService = oauthSessionService;
		this.statReporter = statReporter;
		this.freemarkerHandler = freemarkerHandler;
	}

	public void returnOauthResponse(AuthorizationResponse oauthResponse, boolean destroySession) throws EopException
	{
		returnOauthResponseNotThrowing(oauthResponse, destroySession);
		throw new EopException();
	}

	public void returnOauthResponseAndReportStatistic(AuthorizationResponse oauthResponse, boolean destroySession,
			OAuthAuthzContext ctx, Status status) throws EopException
	{
		returnOauthResponseNotThrowingAndReportStatistic(oauthResponse, destroySession, ctx, status);
		throw new EopException();
	}

	public void returnOauthResponseNotThrowing(AuthorizationResponse oauthResponse, boolean destroySession)
	{
		VaadinSession session = VaadinSession.getCurrent();
		session.addRequestHandler(new SendResponseRequestHandler(destroySession));
		session.getSession().setAttribute(AuthorizationResponse.class.getName(), oauthResponse);
		UI.getCurrent().getPage().reload();
	}

	public void returnOauthResponseNotThrowingAndReportStatistic(AuthorizationResponse oauthResponse,
			boolean destroySession, OAuthAuthzContext ctx, Status status)
	{
		returnOauthResponseNotThrowing(oauthResponse, destroySession);
		statReporter.reportStatus(ctx, status);
	}

	public class SendResponseRequestHandler extends SynchronizedRequestHandler
	{
		private boolean destroySession;

		public SendResponseRequestHandler(boolean destroySession)
		{
			this.destroySession = destroySession;
		}

		@Override
		public boolean synchronizedHandleRequest(VaadinSession session, VaadinRequest request, VaadinResponse responseO)
				throws IOException
		{
			VaadinServletResponse response = (VaadinServletResponse) responseO;
			AuthorizationResponse oauthResponse = (AuthorizationResponse) session.getSession()
					.getAttribute(AuthorizationResponse.class.getName());
			if (oauthResponse != null)
			{
				Optional<LoginInProgressService.SignInContextSession> sessionAttributes = LoginInProgressService.VaadinContextSession.getCurrent();
				oauthSessionService.cleanupBeforeResponseSent(sessionAttributes);
				try
				{
					Map<String, String> data = new HashMap<>();
					data.put("redirectURL", oauthResponse.toURI().toString());
					
					response.setContentType("application/xhtml+xml; charset=utf-8");
					PrintWriter writer = response.getWriter();
					freemarkerHandler.printGenericPage(writer, "oauthFinish.ftl", data);
				} finally
				{
					oauthSessionService.cleanupAfterResponseSent(sessionAttributes, destroySession);
				}
				return true;
			}

			return false;
		}
	}
}
