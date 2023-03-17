/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import com.nimbusds.oauth2.sdk.*;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.api.utils.RoutingServlet;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthErrorResponseException;
import pl.edu.icm.unity.oauth.as.OAuthIdpStatisticReporter;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.webui.VaadinRequestMatcher;
import pl.edu.icm.unity.webui.idpcommon.EopException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import static pl.edu.icm.unity.webui.LoginInProgressService.noSignInContextException;

/**
 * Invoked after authentication, main OAuth AS servlet. It decides whether the
 * request should be processed automatically or with manual consent. This is
 * separated from Vaadin consent app so it is not needlessly loaded in user's
 * browser.
 * 
 * @author K. Benedyczak
 */
public class ASConsentDeciderServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ASConsentDeciderServlet.class);

	private PreferencesManagement preferencesMan;
	private OAuthIdPEngine idpEngine;
	private OAuthSessionService oauthSessionService;
	private String oauthUiServletPath;
	private String authenticationUIServletPath;
	private final OAuthProcessor oauthProcessor;
	private final OAuthIdpStatisticReporter statReporter;
	private final ASConsentDecider consentDecider;

	public ASConsentDeciderServlet(PreferencesManagement preferencesMan, IdPEngine idpEngine,
			OAuthProcessor oauthProcessor, OAuthSessionService oauthSessionService, String oauthUiServletPath,
			String authenticationUIServletPath, EnquiryManagement enquiryManagement,
			PolicyAgreementManagement policyAgreementsMan, OAuthIdpStatisticReporter idpStatisticReporter, MessageSource msg)
	{
		this.oauthProcessor = oauthProcessor;
		this.preferencesMan = preferencesMan;
		this.oauthSessionService = oauthSessionService;
		this.authenticationUIServletPath = authenticationUIServletPath;
		this.idpEngine = new OAuthIdPEngine(idpEngine);
		this.oauthUiServletPath = oauthUiServletPath;
		this.statReporter = idpStatisticReporter;
		this.consentDecider = new ASConsentDecider(enquiryManagement, policyAgreementsMan, msg);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		// if we got this request here it means that this is a request from
		// Authnentication UI
		// which was not reloaded with something new - either regular endpoint UI or
		// navigated away with a redirect.
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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		try
		{
			serviceInterruptible(req, resp);
		} catch (EopException e)
		{
			// OK
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
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(oauthCtx.getReturnURI(),
					OAuth2Error.SERVER_ERROR, oauthCtx.getRequest().getState(),
					oauthCtx.getRequest().impliedResponseMode());
			sendReturnRedirect(oauthResponse, req, resp, true);
			statReporter.reportStatus(oauthCtx, Status.FAILED);
			return;

		}
		if (consentDecider.forceConsentIfConsentPrompt(oauthCtx))
		{
			log.trace("Consent is required for OAuth request, 'consent' prompt was given , forwarding to consent UI");
			RoutingServlet.forwardTo(oauthUiServletPath, req, resp);
		} 
		else if (consentDecider.isInteractiveUIRequired(preferences, oauthCtx))
		{
			if (consentDecider.isNonePrompt(oauthCtx))
			{
				sendNonePromptError(oauthCtx, req, resp);
				return;
			}
			
			log.trace("Consent is required for OAuth request, forwarding to consent UI");
			RoutingServlet.forwardTo(oauthUiServletPath, req, resp);
		} else
		{
			log.trace("Consent is not required for OAuth request, processing immediatelly");
			autoReplay(preferences, oauthCtx, req, resp);
		}
	}
	
	private void sendNonePromptError(OAuthAuthzContext oauthCtx, HttpServletRequest req, HttpServletResponse resp)
			throws IOException
	{
		log.error("Consent is required but 'none' prompt was given");
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(oauthCtx.getReturnURI(),
				OAuth2Error.SERVER_ERROR, oauthCtx.getRequest().getState(),
				oauthCtx.getRequest().impliedResponseMode());
		sendReturnRedirect(oauthResponse, req, resp, true);
	}

	protected OAuthClientSettings loadPreferences(OAuthAuthzContext oauthCtx) throws EngineException
	{
		OAuthPreferences preferences = OAuthPreferences.getPreferences(preferencesMan);
		return preferences.getSPSettings(oauthCtx.getRequest().getClientID().getValue());
	}

	/**
	 * Automatically sends an OAuth response, without the consent screen.
	 */
	protected void autoReplay(OAuthClientSettings clientPreferences, OAuthAuthzContext oauthCtx,
			HttpServletRequest request, HttpServletResponse response) throws EopException, IOException
	{
		if (!clientPreferences.isDefaultAccept())
		{
			log.trace("User preferences are set to decline authZ from the client");
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(oauthCtx.getReturnURI(),
					OAuth2Error.ACCESS_DENIED, oauthCtx.getRequest().getState(),
					oauthCtx.getRequest().impliedResponseMode());
			statReporter.reportStatus(oauthCtx, Status.FAILED);

			sendReturnRedirect(oauthResponse, request, response, false);
		}

		AuthorizationSuccessResponse respDoc;
		try
		{
			TranslationResult userInfo = idpEngine.getUserInfo(oauthCtx);
			handleTranslationProfileRedirectIfNeeded(userInfo, request, response);
			IdentityParam selectedIdentity = idpEngine.getIdentity(userInfo,
					oauthCtx.getConfig().getSubjectIdentityType());
			log.info("Authentication of " + selectedIdentity);
			Collection<DynamicAttribute> attributes = OAuthProcessor.filterAttributes(userInfo,
					oauthCtx.getEffectiveRequestedAttrs());
			respDoc = oauthProcessor.prepareAuthzResponseAndRecordInternalState(attributes, selectedIdentity, oauthCtx,
					statReporter);
		} catch (OAuthErrorResponseException e)
		{

			statReporter.reportStatus(oauthCtx, Status.FAILED);

			sendReturnRedirect(e.getOauthResponse(), request, response, e.isInvalidateSession());
			return;
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(oauthCtx.getReturnURI(),
					OAuth2Error.SERVER_ERROR, oauthCtx.getRequest().getState(),
					oauthCtx.getRequest().impliedResponseMode());
			statReporter.reportStatus(oauthCtx, Status.FAILED);
			sendReturnRedirect(oauthResponse, request, response, false);
			return;
		}
		sendReturnRedirect(respDoc, request, response, false);
	}

	private void handleTranslationProfileRedirectIfNeeded(TranslationResult userInfo, HttpServletRequest request,
			HttpServletResponse response) throws IOException, EopException
	{
		String redirectURL = userInfo.getRedirectURL();
		if (redirectURL != null)
		{
			response.sendRedirect(redirectURL);
			oauthSessionService.cleanupComplete(Optional.of(new LoginInProgressService.HttpContextSession(request)), false);
			throw new EopException();
		}
	}

	private OAuthAuthzContext getOAuthContext(HttpServletRequest req)
	{
		return OAuthSessionService.getContext(req).orElseThrow(noSignInContextException());
	}

	private void sendReturnRedirect(AuthorizationResponse oauthResponse, HttpServletRequest request,
			HttpServletResponse response, boolean invalidateSession) throws IOException
	{
		LoginInProgressService.SignInContextSession session = new LoginInProgressService.HttpContextSession(request);
		oauthSessionService.cleanupBeforeResponseSent(session);
		try
		{
			String redirectURL = oauthResponse.toURI().toString();
			log.trace("Sending OAuth reply via return redirect: " + redirectURL);
			response.sendRedirect(redirectURL);
		} catch (SerializeException e)
		{
			throw new IOException("Error: can not serialize error response", e);
		} finally
		{
			oauthSessionService.cleanupAfterResponseSent(session, invalidateSession);
		}
	}
}
