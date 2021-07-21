/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import static pl.edu.icm.unity.webui.LoginInProgressService.noSignInContextException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.security.dsig.DSigException;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.RoutingServlet;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SAMLSessionParticipant;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.idp.web.SamlSessionService;
import pl.edu.icm.unity.saml.slo.SamlRoutableMessage;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.LoginInProgressService.HttpContextSession;
import pl.edu.icm.unity.webui.VaadinRequestMatcher;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;

/**
 * Invoked after authentication, main SAML web IdP servlet. It decides whether the request should be
 * processed automatically or with manual consent. This is separated from Vaadin consent app so it is not needlessly 
 * loaded in user's browser.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
@Primary
public class IdpConsentDeciderServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, IdpConsentDeciderServlet.class);
	
	protected PreferencesManagement preferencesMan;
	protected IdPEngine idpEngine;
	protected SSOResponseHandler ssoResponseHandler;
	protected SessionManagement sessionMan;
	protected String samlUiServletPath;
	private String authenticationUIServletPath;
	protected AttributeTypeSupport aTypeSupport;
	private EnquiryManagement enquiryManagement;
	private final PolicyAgreementManagement policyAgreementsMan;
	private final MessageSource msg;
	
	@Autowired
	public IdpConsentDeciderServlet(AttributeTypeSupport aTypeSupport, 
			PreferencesManagement preferencesMan, 
			IdPEngine idpEngine,
			FreemarkerAppHandler freemarker,
			SessionManagement sessionMan,
			@Qualifier("insecure") EnquiryManagement enquiryManagement,
			PolicyAgreementManagement policyAgreementsMan,
			MessageSource msg)
	{
		this.aTypeSupport = aTypeSupport;
		this.preferencesMan = preferencesMan;
		this.idpEngine = idpEngine;
		this.enquiryManagement = enquiryManagement;
		this.ssoResponseHandler = new SSOResponseHandler(freemarker);
		this.sessionMan = sessionMan;
		this.policyAgreementsMan = policyAgreementsMan;
		this.msg = msg;
	}

	protected void init(String samlUiServletPath, String authenticationUIServletPath)
	{
		this.samlUiServletPath = samlUiServletPath;
		this.authenticationUIServletPath = authenticationUIServletPath;
	}
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		//if we got this request here it means that this is a request from Authentication UI
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
			AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(aTypeSupport, samlCtx, 
					Calendar.getInstance(TimeZone.getTimeZone("UTC")));
			String serviceUrl = getServiceUrl(samlCtx);
			ssoResponseHandler.handleException(samlProcessor, e1, Binding.HTTP_POST, 
					serviceUrl, samlCtx.getRelayState(), req, resp, true);
			return;

		}
		if (isInteractiveUIRequired(preferences, samlCtx))
		{
			log.trace("Interactive step is required for SAML request, forwarding to UI");
			RoutingServlet.forwardTo(samlUiServletPath, req, resp);
		} else
		{
			log.trace("Consent is not required for SAML request, processing immediatelly");
			autoReplay(preferences, samlCtx, req, resp);
		}
	}
	
	protected SPSettings loadPreferences(SAMLAuthnContext samlCtx) throws EngineException
	{
		SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan);
		return preferences.getSPSettings(samlCtx.getRequest().getIssuer());
	}


	private boolean isInteractiveUIRequired(SPSettings preferences, SAMLAuthnContext samlCtx)
	{
		return isConsentRequired(preferences, samlCtx) || isActiveValueSelectionRequired(samlCtx) ||
				isEnquiryWaiting() || isPolicyAgreementWaiting(samlCtx);
	}

	
	private boolean isActiveValueSelectionRequired(SAMLAuthnContext samlCtx)
	{
		AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(aTypeSupport, samlCtx, 
				Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		SamlIdpProperties config = samlCtx.getSamlConfiguration();
		return CommonIdPProperties.isActiveValueSelectionConfiguredForClient(config, 
						samlProcessor.getRequestIssuer());
	}
	
	private boolean isConsentRequired(SPSettings preferences, SAMLAuthnContext samlCtx)
	{
		if (preferences.isDoNotAsk())
			return false;
		
		boolean skipConsent = samlCtx.getSamlConfiguration().getBooleanValue(
				CommonIdPProperties.SKIP_CONSENT);
		if (skipConsent)
			return false;
		
		return true;
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

	private boolean isPolicyAgreementWaiting(SAMLAuthnContext samlCtx)
	{
		try
		{
			return !policyAgreementsMan.filterAgreementToPresent(
					new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId()),
					CommonIdPProperties.getPolicyAgreementsConfig(msg,
							samlCtx.getSamlConfiguration()).agreements)
					.isEmpty();
		} catch (EngineException e)
		{
			log.error("Unable to determine policy agreements to accept");
		}
		return false;
	}
	
	/**
	 * Automatically sends a SAML response, without the consent screen.
	 */
	protected void autoReplay(SPSettings spPreferences, SAMLAuthnContext samlCtx, HttpServletRequest request,
			HttpServletResponse response) throws EopException, IOException
	{
		AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(aTypeSupport, samlCtx, 
				Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		
		String serviceUrl = getServiceUrl(samlCtx);
		
		if (!spPreferences.isDefaultAccept())
		{
			AuthenticationException ea = new AuthenticationException("Authentication was declined");
			ssoResponseHandler.handleException(samlProcessor, ea, Binding.HTTP_POST, 
					serviceUrl, samlCtx.getRelayState(), request, response, false);
		}
		
		SamlRoutableMessage respDoc;
		try
		{
			TranslationResult userInfo = getUserInfo(samlCtx.getSamlConfiguration(), samlProcessor, 
					SAMLConstants.BINDING_HTTP_POST);
			handleRedirectIfNeeded(userInfo, request, response);
			IdentityParam selectedIdentity = getIdentity(userInfo, samlProcessor, spPreferences);
			log.info("Authentication of " + selectedIdentity);
			Collection<Attribute> attributes = samlProcessor.getAttributes(userInfo, spPreferences);
			respDoc = samlProcessor.processAuthnRequestReturningResponse(selectedIdentity, attributes, 
					samlCtx.getRelayState(), samlCtx.getResponseDestination());
		} catch (Exception e)
		{
			ssoResponseHandler.handleException(samlProcessor, e, Binding.HTTP_POST, 
					serviceUrl, samlCtx.getRelayState(), request, response, false);
			return;
		}
		addSessionParticipant(samlCtx, samlProcessor.getAuthenticatedSubject().getNameID(), 
				samlProcessor.getSessionId(), sessionMan);
		
		try
		{
			ssoResponseHandler.sendResponse(respDoc, Binding.HTTP_POST, request, response);
		} catch (DSigException e)
		{
			ssoResponseHandler.handleException(samlProcessor, e, Binding.HTTP_POST, 
					serviceUrl, samlCtx.getRelayState(), request, response, false);
		}
	}
	
	private void handleRedirectIfNeeded(TranslationResult userInfo, HttpServletRequest request,
			HttpServletResponse response) 
			throws IOException, EopException
	{
		String redirectURL = userInfo.getRedirectURL();
		if (redirectURL != null)
		{
			response.sendRedirect(redirectURL);
			SamlSessionService.cleanContext(new HttpContextSession(request));
			throw new EopException();
		}
	}
	
	protected TranslationResult getUserInfo(SamlIdpProperties samlProperties, AuthnResponseProcessor processor,
			String binding) 
			throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		
		return idpEngine.obtainUserInformationWithEnrichingImport(new EntityParam(ae.getEntityId()), 
				processor.getChosenGroup(), samlProperties.getOutputTranslationProfile(), 
				processor.getIdentityTarget(), Optional.empty(), "SAML2", binding,
				processor.isIdentityCreationAllowed(),
				samlProperties);
	}

	
	protected IdentityParam getIdentity(TranslationResult userInfo, AuthnResponseProcessor samlProcessor, 
			SPSettings preferences) 
			throws EngineException, SAMLRequesterException
	{
		List<IdentityParam> validIdentities = samlProcessor.getCompatibleIdentities(userInfo.getIdentities());
		return idpEngine.getIdentity(validIdentities, preferences.getSelectedIdentity());
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
		String serviceUrl = samlCtx.getSamlConfiguration().getReturnAddressForRequester(
					samlCtx.getRequest());
		return serviceUrl;
	}
	
	private SAMLAuthnContext getSamlContext(HttpServletRequest req)
	{
		return SamlSessionService.getContext(req).orElseThrow(noSignInContextException());
	}
	
	@Component
	@Primary
	public static class Factory implements IdpConsentDeciderServletFactory
	{
		@Autowired
		private ObjectFactory<IdpConsentDeciderServlet> factory;
		
		@Override
		public IdpConsentDeciderServlet getInstance(String uiServletPath, String authenticationUIServletPath)
		{
			IdpConsentDeciderServlet ret = factory.getObject();
			ret.init(uiServletPath, authenticationUIServletPath);
			return ret;
		}
	}
}
