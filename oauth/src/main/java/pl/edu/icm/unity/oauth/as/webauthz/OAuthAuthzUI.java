/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties.ActiveValueSelectionConfig;
import pl.edu.icm.unity.engine.api.idp.statistic.IdpStatisticEvent;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthErrorResponseException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementScreen;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import pl.edu.icm.unity.webui.idpcommon.activesel.ActiveValueSelectionScreen;

/**
 * UI of the authorization endpoint. Presents active value selection for
 * attributes if configured. When attributes are obtained then consent screen is
 * presented.
 * 
 * @author K. Benedyczak
 */
@Component("OAuthAuthzUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class OAuthAuthzUI extends UnityEndpointUIBase
{
	private static Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthAuthzUI.class);

	private final MessageSource msg;
	private final OAuthIdPEngine idpEngine;
	private final AttributeHandlerRegistry handlersRegistry;
	private final PreferencesManagement preferencesMan;
	private final StandardWebLogoutHandler authnProcessor;
	private final IdentityTypeSupport idTypeSupport;
	private final AttributeTypeSupport aTypeSupport;
	private final OAuthSessionService oauthSessionService;
	private final OAuthProcessor oauthProcessor;
	private final PolicyAgreementManagement policyAgreementsMan;

	private OAuthResponseHandler oauthResponseHandler;
	private IdentityParam identity;
	private ObjectFactory<PolicyAgreementScreen> policyAgreementScreenObjectFactory;
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	public OAuthAuthzUI(MessageSource msg,
			OAuthProcessor oauthProcessor,
			AttributeHandlerRegistry handlersRegistry,
			PreferencesManagement preferencesMan,
			StandardWebLogoutHandler authnProcessor,
			IdPEngine idpEngine,
			EnquiresDialogLauncher enquiryDialogLauncher,
			IdentityTypeSupport idTypeSupport,
			AttributeTypeSupport aTypeSupport,
			OAuthSessionService oauthSessionService,
			PolicyAgreementManagement policyAgreementsMan,
			ObjectFactory<PolicyAgreementScreen> policyAgreementScreenObjectFactory,
			ApplicationEventPublisher eventPublisher)
	{
		super(msg, enquiryDialogLauncher);
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
		this.policyAgreementScreenObjectFactory = policyAgreementScreenObjectFactory;
		this.eventPublisher = eventPublisher;
	}

	@Override
	protected void enter(VaadinRequest request)
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
		setContent(policyAgreementScreenObjectFactory.getObject()
				.withTitle(config.getLocalizedStringWithoutFallbackToDefault(msg,
						CommonIdPProperties.POLICY_AGREEMENTS_TITLE))
				.withInfo(config.getLocalizedStringWithoutFallbackToDefault(msg,
						CommonIdPProperties.POLICY_AGREEMENTS_INFO))
				.withAgreements(filterAgreementToPresent)
				.withWidht(config.getLongValue(CommonIdPProperties.POLICY_AGREEMENTS_WIDTH),
						config.getValue(CommonIdPProperties.POLICY_AGREEMENTS_WIDTH_UNIT))
				.withSubmitHandler(() -> activeValueSelectionAndConsentStage(ctx, config)));
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

		Optional<ActiveValueSelectionConfig> activeValueSelectionConfig = CommonIdPProperties
				.getActiveValueSelectionConfig(config, ctx.getClientUsername(), allAttributes);

		if (activeValueSelectionConfig.isPresent())
			showActiveValueSelectionScreen(activeValueSelectionConfig.get());
		else
			gotoConsentStage(allAttributes);
	}

	private void gotoConsentStage(Collection<DynamicAttribute> attributes)
	{
		if (OAuthSessionService.getVaadinContext().getConfig().isSkipConsent())
		{
			onFinalConfirm(identity, attributes);
			return;
		}
		OAuthConsentScreen consentScreen = new OAuthConsentScreen(msg, handlersRegistry, preferencesMan,
				authnProcessor, idTypeSupport, aTypeSupport, identity, attributes,
				this::onDecline, this::onFinalConfirm, oauthSessionService, () -> reportStatus(OAuthSessionService.getVaadinContext(), Status.FAILED));
		setContent(consentScreen);
	}

	private void showActiveValueSelectionScreen(ActiveValueSelectionConfig config)
	{
		ActiveValueSelectionScreen valueSelectionScreen = new ActiveValueSelectionScreen(msg, handlersRegistry,
				authnProcessor, config.singleSelectableAttributes, config.multiSelectableAttributes,
				config.remainingAttributes, this::onDecline, this::gotoConsentStage);
		setContent(valueSelectionScreen);
	}

	private TranslationResult getTranslationResult(OAuthAuthzContext ctx) throws EopException
	{
		oauthResponseHandler = new OAuthResponseHandler(oauthSessionService);
		try
		{
			return idpEngine.getUserInfo(ctx);
		} catch (OAuthErrorResponseException e)
		{
			reportStatus(ctx, Status.FAILED);
			oauthResponseHandler.returnOauthResponse(e.getOauthResponse(), e.isInvalidateSession());
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			// we kill the session as the user may want to log as
			// different user
			// if has access to several entities.
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(),
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			reportStatus(ctx, Status.FAILED);
			oauthResponseHandler.returnOauthResponse(oauthResponse, true);
		}
		
		return null; // not reachable
	}

	private void handleRedirectIfNeeded(TranslationResult userInfo) throws EopException
	{
		String redirectURL = userInfo.getRedirectURL();
		if (redirectURL != null)
		{
			Page.getCurrent().open(redirectURL, null);
			throw new EopException();
		}
	}

	private void onDecline()
	{
		OAuthAuthzContext ctx = OAuthSessionService.getVaadinContext();
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(),
				OAuth2Error.ACCESS_DENIED, ctx.getRequest().getState(),
				ctx.getRequest().impliedResponseMode());
		
		reportStatus(ctx, Status.FAILED);
		oauthResponseHandler.returnOauthResponseNotThrowing(oauthResponse, false);
	}

	private void onFinalConfirm(IdentityParam identity, Collection<DynamicAttribute> attributes)
	{
		OAuthAuthzContext ctx = OAuthSessionService.getVaadinContext();
		try
		{
			AuthorizationSuccessResponse oauthResponse = oauthProcessor
					.prepareAuthzResponseAndRecordInternalState(attributes, identity, ctx, endpointDescription);

			oauthResponseHandler.returnOauthResponseNotThrowing(oauthResponse, false);
		} catch (Exception e)
		{
			log.error("Error during OAuth processing", e);
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(),
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			
			reportStatus(ctx, Status.FAILED);
			oauthResponseHandler.returnOauthResponseNotThrowing(oauthResponse, false);
		}
	}
	
	private void reportStatus(OAuthAuthzContext ctx, Status status)
	{
		eventPublisher.publishEvent(new IdpStatisticEvent(endpointDescription.getName(),
				endpointDescription.getEndpoint().getConfiguration().getDisplayedName() != null
						? endpointDescription.getEndpoint().getConfiguration().getDisplayedName().getValue(msg)
						: null,
				ctx.getClientUsername(), ctx.getClientName(), Status.FAILED));
	}
}
