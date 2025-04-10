/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import static io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.noSignInContextException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.ServletApiRequest;
import org.eclipse.jetty.security.AuthenticationState;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.openid.connect.sdk.OIDCError;

import io.imunity.vaadin.endpoint.common.EopException;
import io.imunity.vaadin.endpoint.common.QueryBuilder;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.AttributeValueFilter;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthErrorResponseException;
import pl.edu.icm.unity.oauth.as.OAuthIdpStatisticReporter;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;

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

	private final PreferencesManagement preferencesMan;
	private final OAuthIdPEngine idpEngine;
	private final OAuthSessionService oauthSessionService;
	private final String oauthUiServletPath;
	private final OAuthProcessor oauthProcessor;
	private final OAuthIdpStatisticReporter statReporter;
	private final ASConsentDecider consentDecider;

	public ASConsentDeciderServlet(PreferencesManagement preferencesMan, IdPEngine idpEngine,
			OAuthProcessor oauthProcessor, OAuthSessionService oauthSessionService, String oauthUiServletPath,
			EnquiryManagement enquiryManagement,
			PolicyAgreementManagement policyAgreementsMan, OAuthIdpStatisticReporter idpStatisticReporter, MessageSource msg)
	{
		this.oauthProcessor = oauthProcessor;
		this.preferencesMan = preferencesMan;
		this.oauthSessionService = oauthSessionService;
		this.idpEngine = new OAuthIdPEngine(idpEngine);
		this.oauthUiServletPath = oauthUiServletPath;
		this.statReporter = idpStatisticReporter;
		this.consentDecider = new ASConsentDecider(enquiryManagement, policyAgreementsMan, msg);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		ServletApiRequest apiRequest = (ServletApiRequest) req;
		if (AuthenticationState.getAuthenticationState(apiRequest.getRequest()) == null)
		{
			sendRedirect(req, resp);
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
			log.debug("Consent is required for OAuth request, 'consent' prompt was requested, redirect to consent UI");
			sendRedirect(req, resp);
		} 
		else if (consentDecider.isInteractiveUIRequired(preferences, oauthCtx))
		{
			if (consentDecider.isNonePrompt(oauthCtx))
			{
				sendNonePromptError(oauthCtx, req, resp);
				return;
			}
			
			log.debug("Consent is required for OAuth request, forwarding to consent UI");
			sendRedirect(req, resp);
		} else
		{
			log.debug("Consent is not required for OAuth request, processing immediatelly");
			autoReplay(preferences, oauthCtx, req, resp);
		}
	}
	
	private void sendRedirect(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		resp.sendRedirect(oauthUiServletPath + getQueryToAppend(req));
	}
	
	private String getQueryToAppend(HttpServletRequest req)
	{
		Map<String, List<String>> urlParameters = req.getParameterMap().entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> Arrays.asList(entry.getValue())
			));
		return QueryBuilder.buildQuery(urlParameters);
	}
	
	private void sendNonePromptError(OAuthAuthzContext oauthCtx, HttpServletRequest req, HttpServletResponse resp)
			throws IOException
	{
		log.info("Consent is required but 'none' prompt was requested");
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(oauthCtx.getReturnURI(),
				OIDCError.CONSENT_REQUIRED, oauthCtx.getRequest().getState(),
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
			log.debug("User preferences are set to decline authZ from the client");
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
			Collection<DynamicAttribute> attributes =  OAuthProcessor.filterAttributes(userInfo,
					oauthCtx.getEffectiveRequestedAttrs());
			Set<DynamicAttribute> filteredAttributes = AttributeValueFilter.filterAttributes(oauthCtx.getClaimValueFilters(), attributes);
			EssentialACRConsistencyValidator.verifyEssentialRequestedACRisReturned(oauthCtx, filteredAttributes);
			respDoc = oauthProcessor.prepareAuthzResponseAndRecordInternalState(filteredAttributes, selectedIdentity, oauthCtx,
					statReporter, InvocationContext.getCurrent().getLoginSession().getAuthenticationTime(), oauthCtx.getClaimValueFilters());
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
