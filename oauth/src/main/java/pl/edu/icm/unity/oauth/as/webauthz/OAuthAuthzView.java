/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import static pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint.OAUTH_CONSENT_DECIDER_SERVLET_PATH;
import static pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint.OAUTH_UI_SERVLET_PATH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.openid.connect.sdk.OIDCError;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.UnityViewComponent;
import io.imunity.vaadin.endpoint.common.EopException;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.active_value_select.ActiveValueSelectionScreen;
import io.imunity.vaadin.endpoint.common.api.EnquiresDialogLauncher;
import io.imunity.vaadin.endpoint.common.consent_utils.PolicyAgreementScreen;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.forms.components.WorkflowCompletedComponent;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.layout.WrappedLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClientHelper;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClientHelper.ActiveValueSelectionConfig;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.translation.StopAuthenticationException;
import pl.edu.icm.unity.engine.api.translation.out.AuthenticationFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.Prompt;
import pl.edu.icm.unity.oauth.as.OAuthErrorResponseException;
import pl.edu.icm.unity.oauth.as.OAuthIdpStatisticReporter.OAuthIdpStatisticReporterFactory;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;

@PermitAll
@Route(value = OAUTH_UI_SERVLET_PATH,  layout =WrappedLayout.class)
class OAuthAuthzView extends UnityViewComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthAuthzView.class);

	private final MessageSource msg;
	private final OAuthIdPEngine idpEngine;
	private final AttributeHandlerRegistry handlersRegistry;
	private final PreferencesManagement preferencesMan;
	private final VaddinWebLogoutHandler authnProcessor;
	private final IdentityTypeSupport idTypeSupport;
	private final AttributeTypeSupport aTypeSupport;
	private final OAuthSessionService oauthSessionService;
	private final OAuthProcessor oauthProcessor;
	private final PolicyAgreementManagement policyAgreementsMan;
	private final PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder;
	private final NotificationPresenter notificationPresenter;
	private final VaadinLogoImageLoader imageAccessService;

	private OAuthResponseHandler oauthResponseHandler;
	private IdentityParam identity;
	private final OAuthIdpStatisticReporterFactory idpStatisticReporterFactory;
	private final FreemarkerAppHandler freemarkerHandler;

	@Autowired
	public OAuthAuthzView(MessageSource msg,
	                      OAuthProcessor oauthProcessor,
	                      AttributeHandlerRegistry handlersRegistry,
	                      PreferencesManagement preferencesMan,
	                      VaddinWebLogoutHandler authnProcessor,
	                      IdPEngine idpEngine,
	                      IdentityTypeSupport idTypeSupport,
	                      AttributeTypeSupport aTypeSupport,
	                      OAuthSessionService oauthSessionService,
	                      PolicyAgreementManagement policyAgreementsMan,
	                      OAuthIdpStatisticReporterFactory idpStatisticReporterFactory,
	                      FreemarkerAppHandler freemarkerHandler,
	                      PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder,
	                      EnquiresDialogLauncher enquiresDialogLauncher,
						  VaadinLogoImageLoader imageAccessService,
	                      NotificationPresenter notificationPresenter
	)
	{
		this.msg = msg;
		this.oauthProcessor = oauthProcessor;
		this.handlersRegistry = handlersRegistry;
		this.preferencesMan = preferencesMan;
		this.authnProcessor = authnProcessor;
		this.oauthSessionService = oauthSessionService;
		this.idpEngine = new OAuthIdPEngine(idpEngine);
		this.idTypeSupport = idTypeSupport;
		this.aTypeSupport = aTypeSupport;
		this.policyAgreementsMan = policyAgreementsMan;
		this.idpStatisticReporterFactory = idpStatisticReporterFactory;
		this.freemarkerHandler = freemarkerHandler;
		this.policyAgreementRepresentationBuilder = policyAgreementRepresentationBuilder;
		this.notificationPresenter = notificationPresenter;
		this.imageAccessService = imageAccessService;
		enquiresDialogLauncher.showEnquiryDialogIfNeeded(this::enter);
	}

	protected void enter()
	{
		OAuthAuthzContext ctx = OAuthSessionService.getVaadinContext();
		OAuthASProperties config = ctx.getConfig();

		List<PolicyAgreementConfiguration> filteredAgreementToPresent = filterAgreementsToPresents(config);
		if (!filteredAgreementToPresent.isEmpty())
		{
			policyAgreementsStage(ctx, config, filteredAgreementToPresent);
		} else
		{
			activeValueSelectionAndConsentStage(ctx, config);
		}
	}

	private List<PolicyAgreementConfiguration> filterAgreementsToPresents(OAuthASProperties config)
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

	private void policyAgreementsStage(OAuthAuthzContext ctx, OAuthASProperties config,
			List<PolicyAgreementConfiguration> filterAgreementToPresent)
	{
		getContent().removeAll();
		getContent().add(PolicyAgreementScreen.builder()
				.withMsg(msg)
				.withPolicyAgreementDecider(policyAgreementsMan)
				.withNotificationPresenter(notificationPresenter)
				.withPolicyAgreementRepresentationBuilder(policyAgreementRepresentationBuilder)
				.withTitle(config.getLocalizedStringWithoutFallbackToDefault(msg,
						CommonIdPProperties.POLICY_AGREEMENTS_TITLE))
				.withInfo(config.getLocalizedStringWithoutFallbackToDefault(msg,
						CommonIdPProperties.POLICY_AGREEMENTS_INFO))
				.withAgreements(filterAgreementToPresent)
				.withWidth(config.getLongValue(CommonIdPProperties.POLICY_AGREEMENTS_WIDTH),
						config.getValue(CommonIdPProperties.POLICY_AGREEMENTS_WIDTH_UNIT))
				.withSubmitHandler(() -> activeValueSelectionAndConsentStage(ctx, config))
				.build()
		);
	}

	private void activeValueSelectionAndConsentStage(OAuthAuthzContext ctx, OAuthASProperties config)
	{

		TranslationResult translationResult;
		try
		{
			translationResult = getTranslationResult(ctx);
			handleRedirectIfNeeded(translationResult);
		} catch (EopException e)
		{
			return;
		}

		identity = idpEngine.getIdentity(translationResult, ctx.getConfig().getSubjectIdentityType());

		Set<DynamicAttribute> allAttributes = OAuthProcessor.filterAttributes(translationResult,
				ctx.getEffectiveRequestedAttrs());

		Optional<ActiveValueSelectionConfig> activeValueSelectionConfig = ActiveValueClientHelper
				.getActiveValueSelectionConfig(config.getActiveValueClients(), ctx.getClientUsername(), allAttributes);

		if (activeValueSelectionConfig.isPresent())
			showActiveValueSelectionScreen(activeValueSelectionConfig.get());
		else
			gotoConsentStage(allAttributes);
	}

	private void gotoConsentStage(Collection<DynamicAttribute> attributes)
	{
		OAuthAuthzContext context = OAuthSessionService.getVaadinContext();
		if (!forceConsentIfConsentPrompt(context))
		{
			if (context.getConfig().isSkipConsent())
			{
				onFinalConfirm(identity, attributes);
				return;
			} else if (isNonePrompt(context))
			{
				sendNonePromptError(context);
				return;
			}
		}
		OAuthConsentScreen consentScreen = new OAuthConsentScreen(msg, handlersRegistry, preferencesMan,
				authnProcessor, idTypeSupport, aTypeSupport, identity, attributes,
				this::onDecline, this::onFinalConfirm, oauthResponseHandler);
		getContent().removeAll();
		getContent().add(consentScreen);
	}
	
	private void sendNonePromptError(OAuthAuthzContext oauthCtx)
	{
		log.error("Consent is required but 'none' prompt was given");
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(oauthCtx.getReturnURI(),
				OIDCError.CONSENT_REQUIRED, oauthCtx.getRequest().getState(),
				oauthCtx.getRequest().impliedResponseMode());
		oauthResponseHandler.returnOauthResponseNotThrowing(oauthResponse, true);
	}
	
	private boolean isNonePrompt(OAuthAuthzContext oauthCtx)
	{
		return oauthCtx.getPrompts().contains(Prompt.NONE);	
	}
	
	private boolean forceConsentIfConsentPrompt(OAuthAuthzContext oauthCtx)
	{
		return oauthCtx.getPrompts().contains(Prompt.CONSENT);
	}

	private void showActiveValueSelectionScreen(ActiveValueSelectionConfig config)
	{
		ActiveValueSelectionScreen valueSelectionScreen = new ActiveValueSelectionScreen(msg, handlersRegistry,
				authnProcessor, config.singleSelectableAttributes, config.multiSelectableAttributes,
				config.remainingAttributes, OAUTH_CONSENT_DECIDER_SERVLET_PATH, this::onDecline, this::gotoConsentStage);
		getContent().removeAll();
		getContent().add(valueSelectionScreen);
	}

	private TranslationResult getTranslationResult(OAuthAuthzContext ctx) throws EopException
	{
		oauthResponseHandler = new OAuthResponseHandler(oauthSessionService, 
				idpStatisticReporterFactory.getForEndpoint(Vaadin2XWebAppContext.getCurrentWebAppEndpoint()), freemarkerHandler);
		try
		{
			return idpEngine.getUserInfo(ctx);
		} catch (OAuthErrorResponseException e)
		{
			oauthResponseHandler.returnOauthResponseAndReportStatistic(e.getOauthResponse(), e.isInvalidateSession(), ctx, Status.FAILED);
		} catch (StopAuthenticationException e) {
			handleFinalizationScreen(e.finalizationScreenConfiguration);
		}

		catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			// we kill the session as the user may want to log as
			// different user
			// if has access to several entities.
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(),
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			oauthResponseHandler.returnOauthResponseAndReportStatistic(oauthResponse, true, ctx, Status.FAILED);
		}
		
		return null; // not reachable
	}

	private void handleRedirectIfNeeded(TranslationResult userInfo) throws EopException
	{
		String redirectURL = userInfo.getRedirectURL();
		if (redirectURL != null)
		{
			UI.getCurrent().getPage().open(redirectURL, null);
			throw new EopException();
		}
	}

	private void handleFinalizationScreen(AuthenticationFinalizationConfiguration finalizationScreenConfiguration)
			throws EopException
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
		throw new EopException();
	}

	private void onDecline()
	{
		OAuthAuthzContext ctx = OAuthSessionService.getVaadinContext();
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(),
				OAuth2Error.ACCESS_DENIED, ctx.getRequest().getState(),
				ctx.getRequest().impliedResponseMode());
		
		oauthResponseHandler.returnOauthResponseNotThrowingAndReportStatistic(oauthResponse, false, ctx, Status.FAILED);
	}

	private void onFinalConfirm(IdentityParam identity, Collection<DynamicAttribute> attributes)
	{
		OAuthAuthzContext ctx = OAuthSessionService.getVaadinContext();
		try
		{
			AuthorizationSuccessResponse oauthResponse = oauthProcessor
					.prepareAuthzResponseAndRecordInternalState(attributes, identity, ctx, oauthResponseHandler.statReporter);

			oauthResponseHandler.returnOauthResponseNotThrowing(oauthResponse, false);
		} catch (Exception e)
		{
			log.error("Error during OAuth processing", e);
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(),
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			
			oauthResponseHandler.returnOauthResponseNotThrowingAndReportStatistic(oauthResponse, false, ctx, Status.FAILED);
		}
	}

	@Override
	public String getPageTitle()
	{
		return Vaadin2XWebAppContext.getCurrentWebAppDisplayedName();
	}
}
