/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;

import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties.ActiveValueSelectionConfig;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter.SamlIdpStatisticReporterFactory;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.idp.web.filter.IdpConsentDeciderServlet;
import pl.edu.icm.unity.saml.slo.SamlRoutableSignableMessage;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementScreen;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import pl.edu.icm.unity.webui.idpcommon.activesel.ActiveValueSelectionScreen;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * The main UI of the SAML web IdP. Fairly simple: shows who asks, what is going to be sent,
 * and optionally allows for some customization. This UI is shown always after the user was authenticated
 * and when the SAML request was properly pre-processed.
 *  
 * @author K. Benedyczak
 */
@Component("SamlIdPWebUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class SamlIdPWebUI extends UnityEndpointUIBase implements UnityWebUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlIdPWebUI.class);
	protected MessageSource msg;
	protected IdPEngine idpEngine;
	protected FreemarkerAppHandler freemarkerHandler;
	protected AttributeHandlerRegistry handlersRegistry;
	protected IdentityTypeSupport identityTypeSupport;
	protected PreferencesManagement preferencesMan;
	protected StandardWebLogoutHandler authnProcessor;
	protected SessionManagement sessionMan;
	protected ImageAccessService imageAccessService;
	protected PolicyAgreementManagement policyAgreementsMan;
	private ObjectFactory<PolicyAgreementScreen> policyAgreementScreenObjectFactory;
	
	protected AuthnResponseProcessor samlProcessor;
	protected SamlResponseHandler samlResponseHandler;
	protected AttributeTypeManagement attrTypeMan;
	protected AttributeTypeSupport aTypeSupport;
	protected List<IdentityParam> validIdentities;
	protected Map<String, AttributeType> attributeTypes;
	protected final SamlIdpStatisticReporterFactory idpStatisticReporterFactory;

	@Autowired
	public SamlIdPWebUI(MessageSource msg, ImageAccessService imageAccessService,
			FreemarkerAppHandler freemarkerHandler,
			AttributeHandlerRegistry handlersRegistry, PreferencesManagement preferencesMan,
			StandardWebLogoutHandler authnProcessor, IdPEngine idpEngine,
			IdentityTypeSupport identityTypeSupport, SessionManagement sessionMan, 
			AttributeTypeManagement attrsMan, 
			EnquiresDialogLauncher enquiryDialogLauncher,
			AttributeTypeSupport aTypeSupport,
			PolicyAgreementManagement policyAgreementsMan,
			ObjectFactory<PolicyAgreementScreen> policyAgreementScreenObjectFactory,
			SamlIdpStatisticReporterFactory idpStatisticReporterFactory)
	{
		super(msg, enquiryDialogLauncher);
		this.msg = msg;
		this.imageAccessService = imageAccessService;
		this.freemarkerHandler = freemarkerHandler;
		this.handlersRegistry = handlersRegistry;
		this.preferencesMan = preferencesMan;
		this.authnProcessor = authnProcessor;
		this.idpEngine = idpEngine;
		this.identityTypeSupport = identityTypeSupport;
		this.sessionMan = sessionMan;
		this.attrTypeMan = attrsMan;
		this.aTypeSupport = aTypeSupport;
		this.policyAgreementsMan = policyAgreementsMan;
		this.policyAgreementScreenObjectFactory = policyAgreementScreenObjectFactory;
		this.idpStatisticReporterFactory = idpStatisticReporterFactory;
	}

	protected TranslationResult getUserInfo(SAMLAuthnContext samlCtx, AuthnResponseProcessor processor) 
			throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		return idpEngine.obtainUserInformationWithEnrichingImport(new EntityParam(ae.getEntityId()), 
				processor.getChosenGroup(), samlCtx.getSamlConfiguration().getOutputTranslationProfile(), 
				samlProcessor.getIdentityTarget(), Optional.empty(), 
				"SAML2", SAMLConstants.BINDING_HTTP_REDIRECT,
				processor.isIdentityCreationAllowed(),
				samlCtx.getSamlConfiguration());
	}

	@Override
	protected void enter(VaadinRequest request)
	{
		SAMLAuthnContext samlCtx = SamlSessionService.getVaadinContext();
		SamlIdpProperties samlConfiguration = samlCtx.getSamlConfiguration();
		List<PolicyAgreementConfiguration> filteredAgreementToPresent = filterAgreementsToPresents(samlConfiguration);
		if (!filteredAgreementToPresent.isEmpty())
		{
			policyAgreementsStage(samlCtx, samlConfiguration, filteredAgreementToPresent);
		} else
		{
			activeValueSelectionAndConsentStage(samlCtx, samlConfiguration);
		}
	}
	
	private List<PolicyAgreementConfiguration> filterAgreementsToPresents(SamlIdpProperties config)
	{
		List<PolicyAgreementConfiguration> filterAgreementToPresent = new ArrayList<>();
		try
		{
			filterAgreementToPresent.addAll(policyAgreementsMan.filterAgreementToPresent(
					new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId()),
					CommonIdPProperties.getPolicyAgreementsConfig(msg, config).agreements));
		} catch (EngineException e)
		{
			log.error("Unable to determine policy agreements to accept");
		}
		return filterAgreementToPresent;
	}

	private void policyAgreementsStage(SAMLAuthnContext ctx, SamlIdpProperties config,
			List<PolicyAgreementConfiguration> filterAgreementToPresent)
	{
		setContent(policyAgreementScreenObjectFactory.getObject()
				.withTitle(config.getLocalizedStringWithoutFallbackToDefault(msg,
						CommonIdPProperties.POLICY_AGREEMENTS_TITLE))
				.withInfo(config.getLocalizedStringWithoutFallbackToDefault(msg,
						CommonIdPProperties.POLICY_AGREEMENTS_INFO))
				.withWidht(config.getLongValue(CommonIdPProperties.POLICY_AGREEMENTS_WIDTH),
						config.getValue(CommonIdPProperties.POLICY_AGREEMENTS_WIDTH_UNIT))
				.withAgreements(filterAgreementToPresent)
				.withSubmitHandler(() -> activeValueSelectionAndConsentStage(ctx, config)));
	}
	
	private void activeValueSelectionAndConsentStage(SAMLAuthnContext samlCtx, SamlIdpProperties samlConfiguration)
	{
		samlProcessor = new AuthnResponseProcessor(aTypeSupport, samlCtx, 
				Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		samlResponseHandler = new SamlResponseHandler(freemarkerHandler, samlProcessor, idpStatisticReporterFactory, endpointDescription.getEndpoint());

		TranslationResult translationResult;
		try
		{
			attributeTypes = attrTypeMan.getAttributeTypesAsMap();
			translationResult = getUserInfo(samlCtx, samlProcessor);
			handleRedirectIfNeeded(translationResult);
			validIdentities = samlProcessor.getCompatibleIdentities(translationResult.getIdentities());
		}  catch (EopException eop) 
		{
			return;
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			samlResponseHandler.handleExceptionNotThrowing(e, true);
			return;
		}
		Collection<DynamicAttribute> allAttributes = translationResult.getAttributes();
		
		Optional<ActiveValueSelectionConfig> activeValueSelectionConfig = 
				CommonIdPProperties.getActiveValueSelectionConfig(samlConfiguration, 
						samlProcessor.getRequestIssuer(), allAttributes);
		
		if (activeValueSelectionConfig.isPresent())
			showActiveValueSelectionScreen(activeValueSelectionConfig.get());
		else
			gotoConsentStage(allAttributes);
	}

	protected void gotoConsentStage(Collection<DynamicAttribute> attributes)
	{
		if (SamlSessionService.getVaadinContext().getSamlConfiguration().getBooleanValue(CommonIdPProperties.SKIP_CONSENT))
		{
			onAccepted(validIdentities.get(0), attributes.stream()
					.map(da -> da.getAttribute())
					.collect(Collectors.toList()));
			return;
		}
		
		SamlConsentScreen consentScreen = new SamlConsentScreen(msg, imageAccessService,
				handlersRegistry, 
				preferencesMan, 
				authnProcessor, 
				identityTypeSupport, 
				aTypeSupport, 
				validIdentities, 
				attributes, 
				attributeTypes, 
				this::onDecline, 
				this::onAccepted);
		setContent(consentScreen);
	}

	private void showActiveValueSelectionScreen(ActiveValueSelectionConfig config)
	{
		ActiveValueSelectionScreen valueSelectionScreen = new ActiveValueSelectionScreen(msg, 
				handlersRegistry, authnProcessor, 
				config.singleSelectableAttributes, config.multiSelectableAttributes,
				config.remainingAttributes,
				this::onDecline,
				this::gotoConsentStage);
		setContent(valueSelectionScreen);
	}
	
	private void handleRedirectIfNeeded(TranslationResult userInfo) 
			throws IOException, EopException
	{
		String redirectURL = userInfo.getRedirectURL();
		if (redirectURL != null)
		{
			Page.getCurrent().open(redirectURL, null);
			throw new EopException();
		}
	}
	
	protected void onDecline()
	{
		AuthenticationException ea = new AuthenticationException("Authentication was declined");
		samlResponseHandler.handleExceptionNotThrowing(ea, false);
	}
	
	protected void onAccepted(IdentityParam selectedIdentity, Collection<Attribute> attributes)
	{
		SAMLAuthnContext samlCtx = SamlSessionService.getVaadinContext();
		ResponseDocument respDoc;
		try
		{
			SamlRoutableSignableMessage<ResponseDocument> routableResponse = 
					samlProcessor.processAuthnRequestReturningResponse(selectedIdentity, 
					attributes, samlCtx.getRelayState(), samlCtx.getResponseDestination());
			respDoc = routableResponse.getSignedMessage();
		} catch (Exception e)
		{
			samlResponseHandler.handleExceptionNotThrowing(e, false);
			return;
		}
		addSessionParticipant(samlCtx, samlProcessor.getAuthenticatedSubject().getNameID(), 
				samlProcessor.getSessionId());
		samlResponseHandler.returnSamlResponse(respDoc, Status.SUCCESSFUL);
	}
	
	protected void addSessionParticipant(SAMLAuthnContext samlCtx, NameIDType returnedSubject,
			String sessionId)
	{
		IdpConsentDeciderServlet.addSessionParticipant(samlCtx, returnedSubject, sessionId, sessionMan);
	}
}
