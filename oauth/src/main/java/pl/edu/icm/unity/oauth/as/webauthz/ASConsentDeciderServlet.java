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

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.client.ClientType;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.api.utils.RoutingServlet;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthErrorResponseException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.VaadinRequestMatcher;
import pl.edu.icm.unity.webui.idpcommon.EopException;

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
	private String authenticationUIServletPath;
	private EnquiryManagement enquiryManagement;

	
	public ASConsentDeciderServlet(PreferencesManagement preferencesMan, IdPEngine idpEngine,
			TokensManagement tokensMan, SessionManagement sessionMan,
			String oauthUiServletPath, String authenticationUIServletPath,
			EnquiryManagement enquiryManagement)
	{
		this.tokensMan = tokensMan;
		this.preferencesMan = preferencesMan;
		this.sessionMan = sessionMan;
		this.authenticationUIServletPath = authenticationUIServletPath;
		this.enquiryManagement = enquiryManagement;
		this.idpEngine = new OAuthIdPEngine(idpEngine);
		this.oauthUiServletPath = oauthUiServletPath;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		//if we got this request here it means that this is a request from Authnentication UI
		// which was not reloaded with something new - either regular endpoint UI or navigated away with a redirect. 
		if (VaadinRequestMatcher.isVaadinRequest(req))
		{
			String forwardURI = authenticationUIServletPath;
			if (req.getPathInfo() != null) 
				forwardURI += req.getPathInfo();
			log.debug("Request to Vaadin internal address will be forwarded to authN {}", req.getRequestURI());
			req.getRequestDispatcher(forwardURI).forward(req, resp);
			return;
		}
		super.service(req, resp);
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
		if (isInteractiveUIRequired(preferences, oauthCtx))
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
	
	private boolean isInteractiveUIRequired(OAuthClientSettings preferences, OAuthAuthzContext oauthCtx)
	{
		return isConsentRequired(preferences, oauthCtx) || isActiveValueSelectionRequired(oauthCtx) 
				|| isEnquiryWaiting();
	}

	
	private boolean isActiveValueSelectionRequired(OAuthAuthzContext oauthCtx)
	{
		return CommonIdPProperties.isActiveValueSelectionConfiguredForClient(oauthCtx.getConfig(), 
				oauthCtx.getClientUsername());
	}
	
	private boolean isConsentRequired(OAuthClientSettings preferences, OAuthAuthzContext oauthCtx)
	{
		if (preferences.isDoNotAsk() && oauthCtx.getClientType() != ClientType.PUBLIC)
			return false;
		
		return !oauthCtx.getConfig().isSkipConsent();
	}

	private boolean isEnquiryWaiting()
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		EntityParam entity = new EntityParam(ae.getEntityId());
		try
		{
			return !enquiryManagement.getPendingEnquires(entity).isEmpty();
		} catch (EngineException e)
		{
			log.warn("Can't retrieve pending enquiries for user", e);
			return false;
		}
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
			handleTranslationProfileRedirectIfNeeded(userInfo, request.getSession(), response);
			IdentityParam selectedIdentity = idpEngine.getIdentity(userInfo, 
					oauthCtx.getConfig().getSubjectIdentityType());
			log.debug("Authentication of " + selectedIdentity);
			Collection<DynamicAttribute> attributes = processor.filterAttributes(userInfo, 
					oauthCtx.getEffectiveRequestedAttrs());
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
	
	private void handleTranslationProfileRedirectIfNeeded(TranslationResult userInfo, HttpSession session,
			HttpServletResponse response) 
			throws IOException, EopException
	{
		String redirectURL = userInfo.getRedirectURL();
		if (redirectURL != null)
		{
			response.sendRedirect(redirectURL);
			session.removeAttribute(OAuthParseServlet.SESSION_OAUTH_CONTEXT);
			throw new EopException();
		}
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
