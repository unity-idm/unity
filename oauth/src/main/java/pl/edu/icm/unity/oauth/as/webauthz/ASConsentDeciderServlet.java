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
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import eu.unicore.samly2.exceptions.SAMLRequesterException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.CommonIdPProperties;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.translation.ExecutionFailException;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.RoutingServlet;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
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
	private IdPEngine idpEngine;
	private String oauthUiServletPath;
	
	public ASConsentDeciderServlet(PreferencesManagement preferencesMan, IdPEngine idpEngine,
			FreemarkerHandler freemarker, TokensManagement tokensMan,
			String oauthUiServletPath)
	{
		this.tokensMan = tokensMan;
		this.preferencesMan = preferencesMan;
		this.idpEngine = idpEngine;
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
		
		return !oauthCtx.isSkipConsent();
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
			TranslationResult userInfo = getUserInfo(oauthCtx);
			IdentityParam selectedIdentity = getIdentity(userInfo, oauthCtx.getSubjectIdentityType());
			log.debug("Authentication of " + selectedIdentity);
			Collection<Attribute<?>> attributes = processor.filterAttributes(userInfo, oauthCtx.getRequestedAttrs());
			respDoc = processor.prepareAuthzResponseAndRecordInternalState(attributes, selectedIdentity, 
					oauthCtx, tokensMan);
		} catch (ExecutionFailException e)
		{
			log.debug("Authentication failed due to profile's decision, returning error");
			ErrorObject eo = new ErrorObject("access_denied", 
					e.getMessage(), HTTPResponse.SC_FORBIDDEN);
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(
					oauthCtx.getReturnURI(), 
					eo, 
					oauthCtx.getRequest().getState(),
					oauthCtx.getRequest().impliedResponseMode());
			sendReturnRedirect(oauthResponse, request, response, false);
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
	
	private TranslationResult getUserInfo(OAuthAuthzContext ctx) throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		String flow = ctx.getRequest().getResponseType().impliesCodeFlow() ? 
				GrantFlow.authorizationCode.toString() : GrantFlow.implicit.toString();
		Boolean skipImport = ctx.getProperties().getBooleanValue(CommonIdPProperties.SKIP_USERIMPORT);
		TranslationResult translationResult = idpEngine.obtainUserInformation(new EntityParam(ae.getEntityId()), 
				ctx.getUsersGroup(), 
				ctx.getTranslationProfile(), 
				ctx.getRequest().getClientID().getValue(),
				"OAuth2", 
				flow,
				true,
				!skipImport);
		return translationResult;
	}
	
	protected IdentityParam getIdentity(TranslationResult userInfo, String subjectIdentityType) 
			throws EngineException, SAMLRequesterException
	{
		for (IdentityParam id: userInfo.getIdentities())
			if (subjectIdentityType.equals(id.getTypeId()))
				return id;
		throw new IllegalStateException("There is no " + subjectIdentityType + " identity "
				+ "for the authenticated user, sub claim can not be created. "
				+ "Probably the endpoint is misconfigured.");
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
			httpSession.invalidate();
	}
}
