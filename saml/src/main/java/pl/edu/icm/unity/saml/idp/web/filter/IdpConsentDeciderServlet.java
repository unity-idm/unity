/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SAMLSessionParticipant;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.CommonIdPProperties;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.RoutingServlet;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;

/**
 * Invoked after authentication, main SAML web IdP servlet. It decides whether the request should be
 * processed automatically or with manual consent. This is separated from Vaadin consent app so it is not needlessly 
 * loaded in user's browser.
 * 
 * @author K. Benedyczak
 */
public class IdpConsentDeciderServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, IdpConsentDeciderServlet.class);
	
	protected PreferencesManagement preferencesMan;
	protected IdPEngine idpEngine;
	protected SSOResponseHandler ssoResponseHandler;
	protected SessionManagement sessionMan;
	protected String samlUiServletPath;
	protected AttributeSyntaxFactoriesRegistry attributeSyntaxFactoriesRegistry;
	
	public IdpConsentDeciderServlet(PreferencesManagement preferencesMan, 
			AttributeSyntaxFactoriesRegistry attributeSyntaxFactoriesRegistry,
			IdPEngine idpEngine,
			FreemarkerHandler freemarker,
			SessionManagement sessionMan, String samlUiServletPath)
	{
		this.preferencesMan = preferencesMan;
		this.attributeSyntaxFactoriesRegistry = attributeSyntaxFactoriesRegistry;
		this.idpEngine = idpEngine;
		this.ssoResponseHandler = new SSOResponseHandler(freemarker);
		this.sessionMan = sessionMan;
		this.samlUiServletPath = samlUiServletPath;
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
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
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
		SAMLAuthnContext samlCtx = getSamlContext(req);
		SPSettings preferences;
		try
		{
			preferences = loadPreferences(samlCtx);
		} catch (EngineException e1)
		{
			AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(samlCtx, 
					Calendar.getInstance(TimeZone.getTimeZone("UTC")));
			String serviceUrl = getServiceUrl(samlCtx);
			ssoResponseHandler.handleException(samlProcessor, e1, Binding.HTTP_POST, 
					serviceUrl, samlCtx.getRelayState(), req, resp, true);
			return;

		}
		if (isConsentRequired(preferences, samlCtx))
		{
			log.trace("Consent is required for SAML request, forwarding to consent UI");
			RoutingServlet.forwardTo(samlUiServletPath, req, resp);
		} else
		{
			log.trace("Consent is not required for SAML request, processing immediatelly");
			autoReplay(preferences, samlCtx, req, resp);
		}
	}
	
	protected SPSettings loadPreferences(SAMLAuthnContext samlCtx) throws EngineException
	{
		SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan, 
				attributeSyntaxFactoriesRegistry);
		return preferences.getSPSettings(samlCtx.getRequest().getIssuer());
	}
	
	protected boolean isConsentRequired(SPSettings preferences, SAMLAuthnContext samlCtx)
	{
		if (preferences.isDoNotAsk())
			return false;
		
		boolean skipConsent = samlCtx.getSamlConfiguration().getBooleanValue(
				CommonIdPProperties.SKIP_CONSENT);
		if (skipConsent)
			return false;
		
		return true;
	}
	
	/**
	 * Automatically sends a SAML response, without the consent screen.
	 * @throws IOException 
	 * @throws EopException 
	 */
	protected void autoReplay(SPSettings spPreferences, SAMLAuthnContext samlCtx, HttpServletRequest request,
			HttpServletResponse response) throws EopException, IOException
	{
		AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(samlCtx, 
				Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		
		String serviceUrl = getServiceUrl(samlCtx);
		
		if (!spPreferences.isDefaultAccept())
		{
			AuthenticationException ea = new AuthenticationException("Authentication was declined");
			ssoResponseHandler.handleException(samlProcessor, ea, Binding.HTTP_POST, 
					serviceUrl, samlCtx.getRelayState(), request, response, false);
		}
		
		ResponseDocument respDoc;
		try
		{
			TranslationResult userInfo = getUserInfo(samlCtx.getSamlConfiguration(), samlProcessor, 
					SAMLConstants.BINDING_HTTP_POST);
			IdentityParam selectedIdentity = getIdentity(userInfo, samlProcessor, spPreferences);
			log.debug("Authentication of " + selectedIdentity);
			Collection<Attribute<?>> attributes = samlProcessor.getAttributes(userInfo, spPreferences);
			respDoc = samlProcessor.processAuthnRequest(selectedIdentity, attributes);
		} catch (Exception e)
		{
			ssoResponseHandler.handleException(samlProcessor, e, Binding.HTTP_POST, 
					serviceUrl, samlCtx.getRelayState(), request, response, false);
			return;
		}
		addSessionParticipant(samlCtx, samlProcessor.getAuthenticatedSubject().getNameID(), 
				samlProcessor.getSessionId(), sessionMan);
		ssoResponseHandler.sendResponse(Binding.HTTP_POST, respDoc, serviceUrl, 
				samlCtx.getRelayState(), request, response);
	}
	
	protected TranslationResult getUserInfo(SamlIdpProperties samlProperties, AuthnResponseProcessor processor,
			String binding) 
			throws EngineException
	{
		String profile = samlProperties.getValue(CommonIdPProperties.TRANSLATION_PROFILE);
		boolean skipImport = samlProperties.getBooleanValue(CommonIdPProperties.SKIP_USERIMPORT);

		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		return idpEngine.obtainUserInformation(new EntityParam(ae.getEntityId()), 
				processor.getChosenGroup(), profile, 
				processor.getIdentityTarget(), "SAML2", binding,
				processor.isIdentityCreationAllowed(),
				!skipImport);
	}

	
	protected IdentityParam getIdentity(TranslationResult userInfo, AuthnResponseProcessor samlProcessor, 
			SPSettings preferences) 
			throws EngineException, SAMLRequesterException
	{
		List<IdentityParam> validIdentities = samlProcessor.getCompatibleIdentities(userInfo.getIdentities());
		return IdPEngine.getIdentity(validIdentities, preferences.getSelectedIdentity());
	}
	
	public static void addSessionParticipant(SAMLAuthnContext samlCtx, NameIDType returnedSubject,
			String sessionId, SessionManagement sessionMan)
	{
		String participantId = samlCtx.getRequest().getIssuer().getStringValue();
		SamlIdpProperties samlIdpProperties = samlCtx.getSamlConfiguration();
		String credentialName = samlIdpProperties.getValue(SamlIdpProperties.CREDENTIAL);
		String configKey = samlIdpProperties.getSPConfigKey(samlCtx.getRequest().getIssuer());
		String localIdpSamlId = samlIdpProperties.getValue(SamlIdpProperties.ISSUER_URI);
		Set<String> allowedCerts = samlIdpProperties.getAllowedSpCerts(configKey);
		List<SAMLEndpointDefinition> logoutEndpoints = configKey == null ? 
				new ArrayList<SAMLEndpointDefinition>(0) :
				samlCtx.getSamlConfiguration().getLogoutEndpointsFromStructuredList(configKey);
		sessionMan.addSessionParticipant(new SAMLSessionParticipant(participantId, 
				returnedSubject, sessionId, logoutEndpoints, localIdpSamlId,
				credentialName, allowedCerts));
	}
	
	protected String getServiceUrl(SAMLAuthnContext samlCtx)
	{
		String serviceUrl = samlCtx.getRequestDocument().getAuthnRequest().getAssertionConsumerServiceURL();
		if (serviceUrl == null)
			serviceUrl = samlCtx.getSamlConfiguration().getReturnAddressForRequester(
					samlCtx.getRequest().getIssuer());
		return serviceUrl;
	}
	
	private SAMLAuthnContext getSamlContext(HttpServletRequest req)
	{
		HttpSession httpSession = req.getSession();
		SAMLAuthnContext ret = (SAMLAuthnContext) httpSession.getAttribute(
				SamlParseServlet.SESSION_SAML_CONTEXT);
		if (ret == null)
			throw new IllegalStateException("No SAML context in UI");
		return ret;
	}
}
