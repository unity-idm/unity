/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.SerializeException;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthErrorResponseException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.RoutingServlet;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Invoked after authentication, main OAuth AS servlet. It decides whether the request should be
 * processed automatically or with manual consent. This is separated from Vaadin consent app so it is not needlessly 
 * loaded in user's browser.
 * 
 * @author K. Benedyczak
 */
public class ASConsentDeciderServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ASConsentDeciderServlet.class);
	
	private PreferencesManagement preferencesMan;
	private TokensManagement tokensMan;
	private OAuthIdPEngine idpEngine;
	private SessionManagement sessionMan;
	private String oauthUiServletPath;

	
	public ASConsentDeciderServlet(PreferencesManagement preferencesMan, IdPEngine idpEngine,
			FreemarkerHandler freemarker, TokensManagement tokensMan, SessionManagement sessionMan,
			String oauthUiServletPath)
	{
		this.tokensMan = tokensMan;
		this.preferencesMan = preferencesMan;
		this.sessionMan = sessionMan;
		this.idpEngine = new OAuthIdPEngine(idpEngine);
		this.oauthUiServletPath = oauthUiServletPath;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		try
		{
			serviceInterruptible(req, resp);
		} catch (EopException e)
		{
			//OK
		}
	}
	
	protected void serviceInterruptible(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException, EopException
	{
		OAuthAuthzContext oauthCtx = getOAuthContext(req);
		OAuthClientSettings preferences;
		try
		{
			preferences = loadPreferences(oauthCtx);
		} catch (EngineException e1)
		{
			log.error("Engine problem when handling client request - can not load preferences", e1);
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(
					oauthCtx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR,
					oauthCtx.getRequest().getState(),
					oauthCtx.getRequest().impliedResponseMode());
			sendReturnRedirect(oauthResponse, req, resp, true);
			return;

		}
		if (isConsentRequired(preferences, oauthCtx))
		{
			log.trace("Consent is required for OAuth request, forwarding to consent UI");
			RoutingServlet.forwardTo(oauthUiServletPath, req, resp);
		} else
		{
			log.trace("Consent is not required for OAuth request, processing immediatelly");
			autoReplay(preferences, oauthCtx, req, resp);
		}
	}
	
	protected OAuthClientSettings loadPreferences(OAuthAuthzContext oauthCtx) throws EngineException
	{
		OAuthPreferences preferences = OAuthPreferences.getPreferences(preferencesMan);
		return preferences.getSPSettings(oauthCtx.getRequest().getClientID().getValue());
	}
	
	protected boolean isConsentRequired(OAuthClientSettings preferences, OAuthAuthzContext oauthCtx)
	{
		if (preferences.isDoNotAsk())
			return false;
		
		return !oauthCtx.getConfig().isSkipConsent();
	}
	
	/**
	 * Automatically sends an OAuth response, without the consent screen.
	 * @throws IOException 
	 * @throws EopException 
	 */
	protected void autoReplay(OAuthClientSettings clientPreferences, OAuthAuthzContext oauthCtx, 
			HttpServletRequest request, HttpServletResponse response) throws EopException, IOException
	{
		if (!clientPreferences.isDefaultAccept())
		{
			log.trace("User preferences are set to decline authZ from the client");
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(
					oauthCtx.getReturnURI(), 
					OAuth2Error.ACCESS_DENIED, 
					oauthCtx.getRequest().getState(),
					oauthCtx.getRequest().impliedResponseMode());
			sendReturnRedirect(oauthResponse, request, response, false);
		}
		
		OAuthProcessor processor = new OAuthProcessor();
		AuthorizationSuccessResponse respDoc;
		try
		{
			TranslationResult userInfo = idpEngine.getUserInfo(oauthCtx);
			IdentityParam selectedIdentity = idpEngine.getIdentity(userInfo, 
					oauthCtx.getConfig().getSubjectIdentityType());
			log.debug("Authentication of " + selectedIdentity);
			Collection<DynamicAttribute> attributes = processor.filterAttributes(userInfo, 
					oauthCtx.getRequestedAttrs());
			respDoc = processor.prepareAuthzResponseAndRecordInternalState(attributes, selectedIdentity, 
					oauthCtx, tokensMan);
		} catch (OAuthErrorResponseException e)
		{
			sendReturnRedirect(e.getOauthResponse(), request, response, e.isInvalidateSession());
			return;
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(
					oauthCtx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR, 
					oauthCtx.getRequest().getState(),
					oauthCtx.getRequest().impliedResponseMode());
			sendReturnRedirect(oauthResponse, request, response, false);
			return;
		}
		sendReturnRedirect(respDoc, request, response, false);
	}
	
	private OAuthAuthzContext getOAuthContext(HttpServletRequest req)
	{
		HttpSession httpSession = req.getSession();
		OAuthAuthzContext ret = (OAuthAuthzContext) httpSession.getAttribute(
				OAuthParseServlet.SESSION_OAUTH_CONTEXT);
		if (ret == null)
			throw new IllegalStateException("No OAuth context after authN");
		return ret;
	}
	
	private void sendReturnRedirect(AuthorizationResponse oauthResponse, HttpServletRequest request, 
			HttpServletResponse response, boolean invalidateSession) throws IOException
	{
		try
		{
			String redirectURL = oauthResponse.toURI().toString();
			log.trace("Sending OAuth reply via return redirect: " + redirectURL);
			response.sendRedirect(redirectURL);
		} catch (SerializeException e)
		{
			throw new IOException("Error: can not serialize error response", e);
		}
		
		HttpSession httpSession = request.getSession();
		httpSession.removeAttribute(OAuthParseServlet.SESSION_OAUTH_CONTEXT);
		if (invalidateSession)
		{
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
			sessionMan.removeSession(loginSession.getId(), true);
		}
	}
}
