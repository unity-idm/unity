/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import eu.unicore.samly2.SAMLConstants;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.UnityViewComponent;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.VaadinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.active_value_select.ActiveValueSelectionScreen;
import io.imunity.vaadin.endpoint.common.api.EnquiresDialogLauncher;
import io.imunity.vaadin.endpoint.common.consent_utils.PolicyAgreementScreen;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.forms.components.WorkflowCompletedComponent;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.layout.WrappedLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import jakarta.annotation.security.PermitAll;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClientHelper;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClientHelper.ActiveValueSelectionConfig;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.translation.StopAuthenticationException;
import pl.edu.icm.unity.engine.api.translation.out.AuthenticationFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter.SamlIdpStatisticReporterFactory;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.idp.web.filter.IdpConsentDeciderServlet;
import pl.edu.icm.unity.saml.slo.SamlRoutableSignableMessage;
import io.imunity.vaadin.endpoint.common.WebLogoutHandler;
import io.imunity.vaadin.endpoint.common.EopException;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppEndpoint;
import static pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint.SAML_CONSENT_DECIDER_SERVLET_PATH;
import static pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint.SAML_UI_SERVLET_PATH;

@PermitAll
@Route(value = SAML_UI_SERVLET_PATH,  layout = WrappedLayout.class)
class SamlIdPWebView extends UnityViewComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlIdPWebView.class);
	private final MessageSource msg;
	private final IdPEngine idpEngine;
	private final FreemarkerAppHandler freemarkerHandler;
	private final AttributeHandlerRegistry handlersRegistry;
	private final IdentityTypeSupport identityTypeSupport;
	private final PreferencesManagement preferencesMan;
	private final WebLogoutHandler authnProcessor;
	private final SessionManagement sessionMan;
	private final VaadinLogoImageLoader imageAccessService;
	private final PolicyAgreementManagement policyAgreementsMan;
	private AuthnResponseProcessor samlProcessor;
	private SamlResponseHandler samlResponseHandler;
	private final AttributeTypeManagement attrTypeMan;
	private final AttributeTypeSupport aTypeSupport;
	private final SamlIdpStatisticReporterFactory idpStatisticReporterFactory;
	private final LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;
	private final PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder;
	private final NotificationPresenter notificationPresenter;

	private List<IdentityParam> validIdentities;
	private Map<String, AttributeType> attributeTypes;

	@Autowired
	public SamlIdPWebView(MessageSource msg, VaadinLogoImageLoader imageAccessService,
	                      FreemarkerAppHandler freemarkerHandler,
	                      AttributeHandlerRegistry handlersRegistry, PreferencesManagement preferencesMan,
	                      VaadinWebLogoutHandler authnProcessor, IdPEngine idpEngine,
	                      IdentityTypeSupport identityTypeSupport, SessionManagement sessionMan,
	                      AttributeTypeManagement attrsMan,
	                      AttributeTypeSupport aTypeSupport,
	                      PolicyAgreementManagement policyAgreementsMan,
	                      PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder,
	                      SamlIdpStatisticReporterFactory idpStatisticReporterFactory,
	                      LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement,
	                      EnquiresDialogLauncher enquiresDialogLauncher,
	                      NotificationPresenter notificationPresenter)
	{
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
		this.idpStatisticReporterFactory = idpStatisticReporterFactory;
		this.lastAccessAttributeManagement = lastAccessAttributeManagement;
		this.policyAgreementRepresentationBuilder = policyAgreementRepresentationBuilder;
		this.notificationPresenter = notificationPresenter;
		enquiresDialogLauncher.showEnquiryDialogIfNeeded(this::enter);
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
				samlCtx.getSamlConfiguration().userImportConfigs);
	}

	void enter()
	{
		SAMLAuthnContext samlCtx = SamlSessionService.getVaadinContext();
		SAMLIdPConfiguration samlIdPConfiguration = samlCtx.getSamlConfiguration();
		List<PolicyAgreementConfiguration> filteredAgreementToPresent = filterAgreementsToPresents(samlIdPConfiguration);
		if (!filteredAgreementToPresent.isEmpty())
		{
			policyAgreementsStage(samlCtx, samlIdPConfiguration, filteredAgreementToPresent);
		} else
		{
			activeValueSelectionAndConsentStage(samlCtx, samlIdPConfiguration);
		}
	}
	
	private List<PolicyAgreementConfiguration> filterAgreementsToPresents(SAMLIdPConfiguration config)
	{
		List<PolicyAgreementConfiguration> filterAgreementToPresent = new ArrayList<>();
		try
		{
			filterAgreementToPresent.addAll(policyAgreementsMan.filterAgreementToPresent(
					new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId()),
					config.policyAgreements.agreements));
		} catch (EngineException e)
		{
			log.error("Unable to determine policy agreements to accept");
		}
		return filterAgreementToPresent;
	}

	private void policyAgreementsStage(SAMLAuthnContext ctx, SAMLIdPConfiguration config,
			List<PolicyAgreementConfiguration> filterAgreementToPresent)
	{
		getContent().removeAll();
		getContent().add(PolicyAgreementScreen.builder()
				.withMsg(msg)
				.withPolicyAgreementDecider(policyAgreementsMan)
				.withNotificationPresenter(notificationPresenter)
				.withPolicyAgreementRepresentationBuilder(policyAgreementRepresentationBuilder)
				.withTitle(config.policyAgreements.title)
				.withInfo(config.policyAgreements.information)
				.withWidth(config.policyAgreements.width, config.policyAgreements.widthUnit)
				.withAgreements(filterAgreementToPresent)
				.withSubmitHandler(() -> activeValueSelectionAndConsentStage(ctx, config))
				.build()
		);
	}
	
	private void activeValueSelectionAndConsentStage(SAMLAuthnContext samlCtx, SAMLIdPConfiguration samlIdPConfiguration)
	{
		samlProcessor = new AuthnResponseProcessor(aTypeSupport, lastAccessAttributeManagement, samlCtx, 
				Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		samlResponseHandler = new SamlResponseHandler(freemarkerHandler, samlProcessor, idpStatisticReporterFactory, getCurrentWebAppEndpoint());

		TranslationResult translationResult;
		try
		{
			attributeTypes = attrTypeMan.getAttributeTypesAsMap();
			translationResult = getUserInfo(samlCtx, samlProcessor);
			handleRedirectIfNeeded(translationResult);
			validIdentities = samlProcessor.getCompatibleIdentities(translationResult.getIdentities());
		}
		catch (StopAuthenticationException e) {
			log.info("Authentication stopped due to profile's decision");
			handleFinalizationScreen(e.finalizationScreenConfiguration);
			return;
		}
		catch (EopException eop)
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
				ActiveValueClientHelper.getActiveValueSelectionConfig(samlIdPConfiguration.activeValueClient,
						samlProcessor.getRequestIssuer(), allAttributes);
		
		if (activeValueSelectionConfig.isPresent())
			showActiveValueSelectionScreen(activeValueSelectionConfig.get());
		else
			gotoConsentStage(allAttributes);
	}

	protected void gotoConsentStage(Collection<DynamicAttribute> attributes)
	{
		if (SamlSessionService.getVaadinContext().getSamlConfiguration().skipConsent)
		{
			onAccepted(validIdentities.get(0), attributes.stream()
					.map(DynamicAttribute::getAttribute)
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
		getContent().removeAll();
		getContent().add(consentScreen);
	}

	private void showActiveValueSelectionScreen(ActiveValueSelectionConfig config)
	{
		ActiveValueSelectionScreen valueSelectionScreen = new ActiveValueSelectionScreen(msg,
				handlersRegistry, authnProcessor, 
				config.singleSelectableAttributes, config.multiSelectableAttributes,
				config.remainingAttributes,
				SAML_CONSENT_DECIDER_SERVLET_PATH,
				this::onDecline,
				this::gotoConsentStage);
		getContent().removeAll();
		getContent().add(valueSelectionScreen);
	}
	
	private void handleFinalizationScreen(AuthenticationFinalizationConfiguration finalizationScreenConfiguration)
	{
		WorkflowFinalizationConfiguration config = new WorkflowFinalizationConfiguration(false, false, null, null,
				finalizationScreenConfiguration.title.getValue(msg), finalizationScreenConfiguration.info.getValue(msg),
				finalizationScreenConfiguration.redirectURL,
				finalizationScreenConfiguration.redirectCaption.getValue(msg),
				finalizationScreenConfiguration.redirectAfterTime);

		WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(
				config,
				imageAccessService.loadImageFromUri(config.logoURL).orElse(null)
		);
		getContent().removeAll();
		getContent().add(finalScreen);
	}

	private void handleRedirectIfNeeded(TranslationResult userInfo)
			throws IOException, EopException
	{
		String redirectURL = userInfo.getRedirectURL();
		if (redirectURL != null)
		{
			UI.getCurrent().getPage().open(redirectURL, null);
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

	@Override
	public String getPageTitle()
	{
		return Vaadin2XWebAppContext.getCurrentWebAppDisplayedName();
	}
}
