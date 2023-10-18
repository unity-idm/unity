/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.security.dsig.DSigException;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.ServletApiRequest;
import org.eclipse.jetty.security.AuthenticationState;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.enquiry.EnquirySelector;
import pl.edu.icm.unity.engine.api.enquiry.EnquirySelector.AccessMode;
import pl.edu.icm.unity.engine.api.enquiry.EnquirySelector.Type;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClientHelper;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.session.SessionManagementEE8;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SAMLSessionParticipant;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter.SamlIdpStatisticReporterFactory;
import pl.edu.icm.unity.saml.idp.TrustedServiceProvider;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.idp.web.SamlSessionService;
import pl.edu.icm.unity.saml.slo.SamlRoutableMessage;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;

import java.io.IOException;
import java.util.*;

import static pl.edu.icm.unity.webui.LoginInProgressService.noSignInContextException;

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
	protected SessionManagementEE8 sessionMan;
	protected String samlUiServletPath;
	protected AttributeTypeSupport aTypeSupport;
	private final EnquiryManagement enquiryManagement;
	private final PolicyAgreementManagement policyAgreementsMan;
	private final FreemarkerAppHandler freemarker;
	private final SamlIdpStatisticReporterFactory idpStatisticReporterFactory;
	protected final LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;
	
	@Autowired
	public IdpConsentDeciderServlet(AttributeTypeSupport aTypeSupport, 
			PreferencesManagement preferencesMan, 
			IdPEngine idpEngine,
			FreemarkerAppHandler freemarker,
			SessionManagementEE8 sessionMan,
			@Qualifier("insecure") EnquiryManagement enquiryManagement,
			PolicyAgreementManagement policyAgreementsMan,
			SamlIdpStatisticReporterFactory idpStatisticReporterFactory,
			LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement)
	{
		this.aTypeSupport = aTypeSupport;
		this.preferencesMan = preferencesMan;
		this.idpEngine = idpEngine;
		this.enquiryManagement = enquiryManagement;
		this.sessionMan = sessionMan;
		this.policyAgreementsMan = policyAgreementsMan;
		this.idpStatisticReporterFactory = idpStatisticReporterFactory;
		this.freemarker = freemarker;
		this.lastAccessAttributeManagement = lastAccessAttributeManagement;
	}

	protected void init(String samlUiServletPath, Endpoint endpoint)
	{
		this.samlUiServletPath = samlUiServletPath;
		this.ssoResponseHandler = new SSOResponseHandler(freemarker, idpStatisticReporterFactory, endpoint);
	}
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		ServletRequestWrapper requestWrapper = (ServletRequestWrapper) req;
		ServletApiRequest apiRequest = (ServletApiRequest)requestWrapper.getRequest();
		if (AuthenticationState.getAuthenticationState(apiRequest.getRequest()) == null)
		{
			resp.sendRedirect(samlUiServletPath);
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
			throws IOException, EopException
	{
		SAMLAuthnContext samlCtx = getSamlContext(req);
		SPSettings preferences;
		try
		{
			preferences = loadPreferences(samlCtx);
		} catch (EngineException e1)
		{
			AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(aTypeSupport, lastAccessAttributeManagement, samlCtx, 
					Calendar.getInstance(TimeZone.getTimeZone("UTC")));
			String serviceUrl = getServiceUrl(samlCtx);
			
			ssoResponseHandler.handleException(samlProcessor, e1, Binding.HTTP_POST, 
					serviceUrl, samlCtx, req, resp, true);
			return;

		}
		if (isInteractiveUIRequired(preferences, samlCtx))
		{
			log.trace("Interactive step is required for SAML request, redirect to UI");
			resp.sendRedirect(samlUiServletPath);
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
		AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(aTypeSupport, lastAccessAttributeManagement, samlCtx, 
				Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		SAMLIdPConfiguration config = samlCtx.getSamlConfiguration();
		return ActiveValueClientHelper.isActiveValueSelectionConfiguredForClient(config.activeValueClient,
						samlProcessor.getRequestIssuer());
	}
	
	private boolean isConsentRequired(SPSettings preferences, SAMLAuthnContext samlCtx)
	{
		if (preferences.isDoNotAsk())
			return false;
		
		boolean skipConsent = samlCtx.getSamlConfiguration().skipConsent;
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
			return !enquiryManagement.getAvailableEnquires(entity, EnquirySelector.builder()
					.withAccessMode(AccessMode.NOT_BY_INVITATION_ONLY)
					.withType(Type.REGULAR)
					.build()).isEmpty();
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
							samlCtx.getSamlConfiguration().policyAgreements.agreements)
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
		AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(aTypeSupport, lastAccessAttributeManagement, samlCtx, 
				Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		
		String serviceUrl = getServiceUrl(samlCtx);
		
		if (!spPreferences.isDefaultAccept())
		{
			AuthenticationException ea = new AuthenticationException("Authentication was declined");
			ssoResponseHandler.handleException(samlProcessor, ea, Binding.HTTP_POST, 
					serviceUrl, samlCtx, request, response, false);
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
					serviceUrl, samlCtx, request, response, false);
			return;
		}
		addSessionParticipant(samlCtx, samlProcessor.getAuthenticatedSubject().getNameID(), 
				samlProcessor.getSessionId(), sessionMan);
		
		try
		{
			ssoResponseHandler.sendResponse(samlCtx, respDoc, Binding.HTTP_POST, request, response);

		} catch (DSigException e)
		{	
			ssoResponseHandler.handleException(samlProcessor, e, Binding.HTTP_POST, 
					serviceUrl, samlCtx, request, response, false);
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
			SamlSessionService.cleanContext(new LoginInProgressService.HttpContextSession(request));
			throw new EopException();
		}
	}
	
	protected TranslationResult getUserInfo(SAMLIdPConfiguration samlIdPConfiguration, AuthnResponseProcessor processor,
	                                        String binding)
			throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		
		return idpEngine.obtainUserInformationWithEnrichingImport(new EntityParam(ae.getEntityId()), 
				processor.getChosenGroup(), samlIdPConfiguration.getOutputTranslationProfile(),
				processor.getIdentityTarget(), Optional.empty(), "SAML2", binding,
				processor.isIdentityCreationAllowed(),
				samlIdPConfiguration.userImportConfigs);
	}

	
	protected IdentityParam getIdentity(TranslationResult userInfo, AuthnResponseProcessor samlProcessor, 
			SPSettings preferences) 
			throws EngineException, SAMLRequesterException
	{
		List<IdentityParam> validIdentities = samlProcessor.getCompatibleIdentities(userInfo.getIdentities());
		return idpEngine.getIdentity(validIdentities, preferences.getSelectedIdentity());
	}
	
	public static void addSessionParticipant(SAMLAuthnContext samlCtx, NameIDType returnedSubject,
			String sessionId, SessionManagementEE8 sessionMan)
	{
		String participantId = samlCtx.getRequest().getIssuer().getStringValue();
		SAMLIdPConfiguration samlConfiguration = samlCtx.getSamlConfiguration();
		String credentialName = samlConfiguration.credentialName;
		TrustedServiceProvider config = samlConfiguration.getSPConfig(samlCtx.getRequest().getIssuer());
		String localIdpSamlId = samlConfiguration.issuerURI;
		Set<String> allowedCerts = Optional.ofNullable(config).map(TrustedServiceProvider::getCertificateNames).orElseGet(Set::of);
		List<SAMLEndpointDefinition> logoutEndpoints = config == null ?
				new ArrayList<SAMLEndpointDefinition>(0) :
				config.getLogoutEndpoints();
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
		public IdpConsentDeciderServlet getInstance(String uiServletPath, Endpoint endpoint)
		{
			IdpConsentDeciderServlet ret = factory.getObject();
			ret.init(uiServletPath, endpoint);
			return ret;
		}
	}
}
