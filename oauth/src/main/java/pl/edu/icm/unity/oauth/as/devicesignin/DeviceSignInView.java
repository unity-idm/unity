/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.devicesignin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;

import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.UnityViewComponent;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.VaadinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.active_value_select.ActiveValueSelectionScreen;
import io.imunity.vaadin.endpoint.common.api.EnquiresDialogLauncher;
import io.imunity.vaadin.endpoint.common.consent_utils.PolicyAgreementScreen;
import io.imunity.vaadin.endpoint.common.file.DownloadHandlers;
import io.imunity.vaadin.endpoint.common.forms.components.WorkflowCompletedComponent;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.layout.WrappedLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClientHelper;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClientHelper.ActiveValueSelectionConfig;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.EntityInGroup;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.AttributeFilteringSpec;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthSystemScopeProvider;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;
import pl.edu.icm.unity.oauth.as.token.access.DeviceCodeRepository;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * RFC 8628 §3.3: device sign-in view. Reads an optional {@code ?user_code=} query parameter; if
 * absent, shows a code-entry form. Once a pending device code record is identified, shows a consent
 * screen and, on confirm/deny, updates the record so that the device's poll on {@code /token} can
 * pick up the result. Always requires an authenticated user ({@code @PermitAll} + the standard
 * authentication filter from {@link SecureVaadin2XEndpoint}).
 */
@PermitAll
@Route(value = DeviceSignInWebEndpoint.SERVLET_PATH, layout = WrappedLayout.class)
class DeviceSignInView extends UnityViewComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, DeviceSignInView.class);

	private final MessageSource msg;
	private final OAuthEndpointsCoordinator coordinator;
	private final DeviceCodeRepository deviceCodeRepository;
	private final OAuthIdPEngine idpEngine;
	private final AttributeHandlerRegistry handlersRegistry;
	private final IdentityTypeSupport idTypeSupport;
	private final AttributeTypeSupport aTypeSupport;
	private final VaadinWebLogoutHandler authnProcessor;
	private final OAuthRequestValidatorFactory requestValidatorFactory;
	private final PolicyAgreementManagement policyAgreementsMan;
	private final PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder;
	private final NotificationPresenter notificationPresenter;
	private final EnquiresDialogLauncher enquiresDialogLauncher;
	private final TransactionalRunner tx;
	private final DeviceCodeVerificationThrottle throttle;

	private String deviceCodeValue;
	private DeviceCodeToken parsedToken;
	private OAuthASProperties config;
	private List<DynamicAttribute> activeValueSelectionFilteredAttributes;

	private enum TransitionResult
	{
		APPLIED, ALREADY_PROCESSED, FAILED
	}

	@Autowired
	DeviceSignInView(MessageSource msg, OAuthEndpointsCoordinator coordinator,
			DeviceCodeRepository deviceCodeRepository, IdPEngine idPEngine, AttributeHandlerRegistry handlersRegistry,
			IdentityTypeSupport idTypeSupport, AttributeTypeSupport aTypeSupport,
			VaadinWebLogoutHandler authnProcessor, OAuthRequestValidatorFactory requestValidatorFactory,
			PolicyAgreementManagement policyAgreementsMan,
			PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder,
			NotificationPresenter notificationPresenter, EnquiresDialogLauncher enquiresDialogLauncher,
			TransactionalRunner tx, DeviceCodeVerificationThrottle throttle)
	{
		this.msg = msg;
		this.coordinator = coordinator;
		this.deviceCodeRepository = deviceCodeRepository;
		this.idpEngine = new OAuthIdPEngine(idPEngine);
		this.throttle = throttle;
		this.handlersRegistry = handlersRegistry;
		this.idTypeSupport = idTypeSupport;
		this.aTypeSupport = aTypeSupport;
		this.authnProcessor = authnProcessor;
		this.tx = tx;
		this.requestValidatorFactory = requestValidatorFactory;
		this.policyAgreementsMan = policyAgreementsMan;
		this.policyAgreementRepresentationBuilder = policyAgreementRepresentationBuilder;
		this.notificationPresenter = notificationPresenter;
		this.enquiresDialogLauncher = enquiresDialogLauncher;
	}

	@Override
	public void setParameter(BeforeEvent event, @WildcardParameter String parameter)
	{
		String userCode = event.getLocation()
				.getQueryParameters()
				.getParameters()
				.getOrDefault("user_code", List.of())
				.stream()
				.findFirst()
				.orElse(null);
		enquiresDialogLauncher.showEnquiryDialogIfNeeded(() ->
		{
			if (userCode == null || userCode.isBlank())
				showCodeEntryForm();
			else
				tryResolve(userCode, true);
		});
	}

	private void showCodeEntryForm()
	{
		VerticalLayout layout = new VerticalLayout();
		layout.setAlignItems(Alignment.CENTER);
		H2 title = new H2(msg.getMessage("DeviceSignIn.connectDeviceTitle"));
		Span description = new Span(msg.getMessage("DeviceSignIn.connectDeviceDescription"));

		VerticalLayout codeGroup = new VerticalLayout();
		codeGroup.setAlignItems(Alignment.CENTER);
		codeGroup.setPadding(false);
		codeGroup.setSpacing(false);
		Span codeLabel = new Span(msg.getMessage("DeviceSignIn.enterCode"));
		TextField codeField = new TextField();
		codeGroup.add(codeLabel, codeField);

		Span codeHint = new Span(msg.getMessage("DeviceSignIn.codeHint"));

		Button cancel = new Button(msg.getMessage("cancel"), e -> onCancel());
		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		Button submit = new Button(msg.getMessage("DeviceSignIn.submit"), e -> tryResolve(codeField.getValue(), false));
		submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		submit.addClickShortcut(Key.ENTER);
		HorizontalLayout buttons = new HorizontalLayout(cancel, submit);

		Span warning1 = new Span(msg.getMessage("DeviceSignIn.codeWarning1"));
		Span warning2 = new Span(msg.getMessage("DeviceSignIn.codeWarning2"));

		layout.add(title, description, codeGroup, codeHint, buttons, warning1, warning2);
		getContent().removeAll();
		getContent().add(layout);
	}

	private void onCancel()
	{
		if (parsedToken != null)
		{
			TransitionResult result = applyTransitionAtomically(currentToken ->
			{
				currentToken.setDeviceCodeStatus(DeviceCodeStatus.DENIED);
				return true;
			});
			if (result == TransitionResult.FAILED)
			{
				showError(msg.getMessage("DeviceSignIn.internalError"));
				return;
			}
		}
		showError(msg.getMessage("DeviceSignIn.cancelled"));
	}

	private void tryResolve(String userCode, boolean confirmCodeStep)
	{
		if (userCode == null || userCode.isBlank())
		{
			showError(msg.getMessage("DeviceSignIn.invalidCode"));
			return;
		}

		long entityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		if (throttle.getRemainingBlockedTimeMs(entityId) > 0)
		{
			showError(msg.getMessage("DeviceSignIn.tooManyAttempts"));
			return;
		}

		Optional<Token> tokenOpt = deviceCodeRepository.findByUserCode(userCode);
		if (tokenOpt.isEmpty())
		{
			throttle.unsuccessfulAttempt(entityId);
			showError(msg.getMessage("DeviceSignIn.invalidCode"));
			return;
		}
		Token token = tokenOpt.get();
		DeviceCodeToken parsed = DeviceCodeToken.getInstanceFromJson(token.getContents());

		if (token.getExpires() != null && token.getExpires().before(new Date()))
		{
			throttle.unsuccessfulAttempt(entityId);
			showError(msg.getMessage("DeviceSignIn.expiredCode"));
			return;
		}
		if (parsed.getDeviceCodeStatus() != DeviceCodeStatus.PENDING)
		{
			// not a guessing signal - most likely the user's own code, already acted upon
			showError(msg.getMessage("DeviceSignIn.alreadyProcessed"));
			return;
		}

		Optional<OAuthASProperties> configOpt = coordinator.getDeviceSignInConfig(parsed.getOauthToken().getIssuerUri());
		if (configOpt.isEmpty() || !configOpt.get().isDeviceGrantEnabled())
		{
			showError(msg.getMessage("DeviceSignIn.disabled"));
			return;
		}

		throttle.successfulAttempt(entityId);
		this.deviceCodeValue = token.getValue();
		this.parsedToken = parsed;
		this.config = configOpt.get();
		this.activeValueSelectionFilteredAttributes = null;

		if (confirmCodeStep)
			showConfirmCodeForm(parsed.getUserCode());
		else
			startConsentFlow();
	}

	private void showConfirmCodeForm(String userCode)
	{
		VerticalLayout layout = new VerticalLayout();
		layout.setAlignItems(Alignment.CENTER);
		H2 title = new H2(msg.getMessage("DeviceSignIn.confirmCodeTitle"));
		Span description = new Span(msg.getMessage("DeviceSignIn.confirmCodeDescription"));

		Span code = new Span(userCode);
		code.addClassName(CssClassNames.DEVICE_CODE.getName());

		Button cancel = new Button(msg.getMessage("cancel"), e -> onCancel());
		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		Button confirm = new Button(msg.getMessage("DeviceSignIn.confirmCodeSubmit"), e -> startConsentFlow());
		confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout buttons = new HorizontalLayout(cancel, confirm);

		Span warning = new Span(msg.getMessage("DeviceSignIn.confirmCodeWarning"));

		layout.add(title, description, code, buttons, warning);
		getContent().removeAll();
		getContent().add(layout);
	}

	private void startConsentFlow()
	{
		List<PolicyAgreementConfiguration> toPresent = filterAgreementsToPresent();
		if (!toPresent.isEmpty())
			showPolicyAgreementsScreen(toPresent);
		else
			prepareAndShowConsent();
	}

	private List<PolicyAgreementConfiguration> filterAgreementsToPresent()
	{
		List<PolicyAgreementConfiguration> toPresent = new ArrayList<>();
		try
		{
			toPresent.addAll(policyAgreementsMan.filterAgreementToPresent(
					new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId()),
					CommonIdPProperties.getPolicyAgreementsConfig(msg, config).agreements));
		} catch (EngineException e)
		{
			log.error("Unable to determine policy agreements to accept", e);
		}
		return toPresent;
	}

	private void showPolicyAgreementsScreen(List<PolicyAgreementConfiguration> toPresent)
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
				.withAgreements(toPresent)
				.withWidth(config.getLongValue(CommonIdPProperties.POLICY_AGREEMENTS_WIDTH),
						config.getValue(CommonIdPProperties.POLICY_AGREEMENTS_WIDTH_UNIT))
				.withSubmitHandler(this::prepareAndShowConsent)
				.build());
	}

	private void prepareAndShowConsent()
	{
		try
		{
			OAuthToken oauthToken = parsedToken.getOauthToken();
			OAuthRequestValidator requestValidator = requestValidatorFactory.getOAuthRequestValidator(config);
			Map<String, AttributeExt> clientAttributes = requestValidator
					.getAttributesNoAuthZ(new EntityParam(oauthToken.getClientId()));

			String usersGroup = getUsersGroup(clientAttributes);
			EntityInGroup requesterEntity = new EntityInGroup(config.getValue(OAuthASProperties.CLIENTS_GROUP),
					new EntityParam(oauthToken.getClientId()));
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();

			TranslationResult translationResult = idpEngine.getUserInfoUnsafe(loginSession.getEntityId(),
					oauthToken.getClientUsername(), Optional.of(requesterEntity), usersGroup,
					config.getOutputTranslationProfile(), GrantFlow.deviceCode.toString(), config, null);

			IdentityParam identity = idpEngine.getIdentity(translationResult, config.getSubjectIdentityType());

			Set<String> requestedAttributes = new HashSet<>();
			for (RequestedOAuthScope si : oauthToken.getEffectiveScope())
				requestedAttributes.addAll(si.scopeDefinition().attributes());
			Set<DynamicAttribute> attributes = OAuthProcessor.filterAttributes(translationResult, requestedAttributes);

			Image clientLogo = buildClientLogo(clientAttributes);
			String clientName = oauthToken.getClientName() != null ? oauthToken.getClientName()
					: oauthToken.getClientUsername();

			Optional<ActiveValueSelectionConfig> activeValueSelectionConfig = ActiveValueClientHelper
					.getActiveValueSelectionConfig(config.getActiveValueClients(), oauthToken.getClientUsername(),
							attributes);

			if (activeValueSelectionConfig.isPresent())
				showActiveValueSelectionScreen(activeValueSelectionConfig.get(), identity, clientName, clientLogo);
			else
				buildAndShowConsentScreen(identity, clientName, clientLogo, attributes, null);
		} catch (Exception e)
		{
			log.error("Error while preparing the device sign-in consent screen", e);
			showError(msg.getMessage("DeviceSignIn.internalError"));
		}
	}

	private void showActiveValueSelectionScreen(ActiveValueSelectionConfig activeValueSelectionConfig,
			IdentityParam identity, String clientName, Image clientLogo)
	{
		ActiveValueSelectionScreen selectionScreen = new ActiveValueSelectionScreen(msg, handlersRegistry,
				authnProcessor, activeValueSelectionConfig.singleSelectableAttributes,
				activeValueSelectionConfig.multiSelectableAttributes, activeValueSelectionConfig.remainingAttributes,
				DeviceSignInWebEndpoint.SERVLET_PATH, this::onDeny,
				selectionResult -> buildAndShowConsentScreen(identity, clientName, clientLogo,
						selectionResult.allAttributes(), selectionResult.filteredAttributes()));
		getContent().removeAll();
		getContent().add(selectionScreen);
	}

	private void buildAndShowConsentScreen(IdentityParam identity, String clientName, Image clientLogo,
			Collection<DynamicAttribute> attributes, List<DynamicAttribute> activeValueFilteredAttributes)
	{
		this.activeValueSelectionFilteredAttributes = activeValueFilteredAttributes;
		DeviceSignInConsentScreen consentScreen = new DeviceSignInConsentScreen(msg, handlersRegistry, authnProcessor,
				idTypeSupport, clientName, clientLogo, parsedToken.getOauthToken().getEffectiveScope(), identity,
				attributes, DeviceSignInWebEndpoint.SERVLET_PATH, this::onDeny, this::onAccept);
		getContent().removeAll();
		getContent().add(consentScreen);
	}

	private String getUsersGroup(Map<String, AttributeExt> clientAttributes)
	{
		AttributeExt groupA = clientAttributes.get(OAuthSystemAttributesProvider.PER_CLIENT_GROUP);
		return groupA != null ? (String) groupA.getValues().get(0) : config.getValue(OAuthASProperties.USERS_GROUP);
	}

	private Image buildClientLogo(Map<String, AttributeExt> clientAttributes)
	{
		Attribute logoAttr = clientAttributes.get(OAuthSystemAttributesProvider.CLIENT_LOGO);
		if (logoAttr == null || !ImageAttributeSyntax.ID.equals(logoAttr.getValueSyntax()))
			return null;
		ImageAttributeSyntax syntax = (ImageAttributeSyntax) aTypeSupport.getSyntax(logoAttr);
		UnityImage image = syntax.convertFromString(logoAttr.getValues().get(0).toString());
		String filename = "%s.%s".formatted(UUID.randomUUID(), image.getType().toExt());
		return new Image(DownloadHandlers.forUnityImage(image, filename), "");
	}

	private void onAccept(IdentityParam identity, Collection<DynamicAttribute> attributes)
	{
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		UserInfo userInfo = OAuthProcessor.prepareUserInfoClaimSet(identity.getValue(), attributes);
		List<AttributeFilteringSpec> activeValueFilters = activeValueSelectionFilteredAttributes == null ? null
				: mapSelectedAttributesToFilters(activeValueSelectionFilteredAttributes);

		TransitionResult result = applyTransitionAtomically(currentToken ->
		{
			OAuthToken oauthToken = currentToken.getOauthToken();
			oauthToken.setSubject(identity.getValue());
			oauthToken.setUserInfo(userInfo.toJSONObject().toJSONString());
			oauthToken.setAuthenticationTime(loginSession.getAuthenticationTime());
			oauthToken.setTokenValidity(config.getAccessTokenValidity());
			oauthToken.setMaxExtendedValidity(config.getMaxExtendedAccessTokenValidity());
			oauthToken.setAudience(List.of(oauthToken.getClientUsername()));
			if (activeValueFilters != null)
				oauthToken.setAttributeValueFilters(activeValueFilters);

			if (!signAndRecordIdTokenIfRequested(oauthToken, userInfo))
				return false;

			currentToken.setSubjectEntityId(loginSession.getEntityId());
			currentToken.setDeviceCodeStatus(DeviceCodeStatus.APPROVED);
			return true;
		});

		switch (result)
		{
		case APPLIED -> showCompleted(true);
		case ALREADY_PROCESSED -> showError(msg.getMessage("DeviceSignIn.alreadyProcessed"));
		case FAILED -> showError(msg.getMessage("DeviceSignIn.internalError"));
		}
	}

	private List<AttributeFilteringSpec> mapSelectedAttributesToFilters(Collection<DynamicAttribute> attributes)
	{
		return attributes.stream()
				.map(a -> new AttributeFilteringSpec(a.getAttribute().getName(),
						a.getAttribute().getValues().stream().collect(Collectors.toSet())))
				.toList();
	}

	/**
	 * Mirrors {@code OAuthProcessor}'s id token generation for the authorization_code flow: an OIDC
	 * ID token is minted once, at consent time, and carried in {@code openidInfo} for later reuse
	 * (device_code grant handling and any subsequent refresh both just decode it, they never sign a
	 * fresh one). Without this, clients requesting the {@code openid} scope would silently get an
	 * OAuth-only response, even though discovery advertises OIDC support for the device grant.
	 */
	private boolean signAndRecordIdTokenIfRequested(OAuthToken oauthToken, UserInfo userInfo)
	{
		boolean openIdRequested = oauthToken.getEffectiveScope()
				.stream()
				.anyMatch(s -> OAuthSystemScopeProvider.OPENID_SCOPE.equals(s.scope()));
		if (!openIdRequested)
			return true;

		try
		{
			Date now = new Date();
			IDTokenClaimsSet idToken = new IDTokenClaimsSet(new Issuer(config.getIssuerName()),
					new Subject(oauthToken.getSubject()),
					oauthToken.getAudience().stream().filter(a -> a != null).map(Audience::new).toList(),
					new Date(now.getTime() + config.getIdTokenValidity() * 1000L), now);
			idToken.setAuthenticationTime(Date.from(oauthToken.getAuthenticationTime()));
			if (oauthToken.hasSupportAttributesInIdToken().orElse(false))
				idToken.putAll(userInfo);

			JWT idTokenSigned = config.getTokenSigner().sign(idToken);
			oauthToken.setOpenidToken(idTokenSigned.serialize());
			return true;
		} catch (Exception e)
		{
			log.error("Cannot create the OpenID Connect ID token for the device code grant", e);
			return false;
		}
	}

	private void onDeny()
	{
		TransitionResult result = applyTransitionAtomically(currentToken ->
		{
			currentToken.setDeviceCodeStatus(DeviceCodeStatus.DENIED);
			return true;
		});
		switch (result)
		{
		case APPLIED -> showCompleted(false);
		case ALREADY_PROCESSED -> showError(msg.getMessage("DeviceSignIn.alreadyProcessed"));
		case FAILED -> showError(msg.getMessage("DeviceSignIn.internalError"));
		}
	}

	/**
	 * Atomically re-reads the pending record under a database row lock, applies {@code mutator} only
	 * if it is still PENDING, and writes it back - all within one transaction. This closes two
	 * races: a concurrent /token poll transitioning or removing the same record, and a second
	 * browser session (duplicate tab, or resubmission) trying to accept/deny/cancel a record that
	 * was already decided.
	 */
	private TransitionResult applyTransitionAtomically(Predicate<DeviceCodeToken> mutator)
	{
		try
		{
			return tx.runInTransactionRet(() ->
			{
				Optional<Token> current = deviceCodeRepository.getByDeviceCodeForUpdate(deviceCodeValue);
				if (current.isEmpty())
					return TransitionResult.ALREADY_PROCESSED;
				DeviceCodeToken currentToken = DeviceCodeToken.getInstanceFromJson(current.get().getContents());
				if (currentToken.getDeviceCodeStatus() != DeviceCodeStatus.PENDING)
					return TransitionResult.ALREADY_PROCESSED;
				if (!mutator.test(currentToken))
					return TransitionResult.FAILED;
				try
				{
					deviceCodeRepository.update(deviceCodeValue, currentToken, null);
				} catch (JsonProcessingException e)
				{
					throw new RuntimeException(e);
				}
				return TransitionResult.APPLIED;
			});
		} catch (Exception e)
		{
			log.error("Can not update the device code record", e);
			return TransitionResult.FAILED;
		}
	}

	private void showCompleted(boolean success)
	{
		WorkflowFinalizationConfiguration finalConfig = WorkflowFinalizationConfiguration.builder()
				.setSuccess(success)
				.setMainInformation(msg.getMessage(success ? "DeviceSignIn.completed" : "DeviceSignIn.denied"))
				.setExtraInformation(msg.getMessage("DeviceSignIn.canCloseWindow"))
				.build();
		getContent().removeAll();
		getContent().add(new WorkflowCompletedComponent(finalConfig, null));
	}

	private void showError(String message)
	{
		WorkflowFinalizationConfiguration finalConfig = WorkflowFinalizationConfiguration.builder()
				.setSuccess(false)
				.setMainInformation(message)
				.build();
		getContent().removeAll();
		getContent().add(new WorkflowCompletedComponent(finalConfig, null));
	}

	@Override
	public String getPageTitle()
	{
		return Vaadin2XWebAppContext.getCurrentWebAppDisplayedName();
	}
}
