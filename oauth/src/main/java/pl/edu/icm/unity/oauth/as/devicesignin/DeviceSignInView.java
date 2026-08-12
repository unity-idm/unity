/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.devicesignin;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import io.imunity.vaadin.elements.UnityViewComponent;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.VaadinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.file.DownloadHandlers;
import io.imunity.vaadin.endpoint.common.forms.components.WorkflowCompletedComponent;
import io.imunity.vaadin.endpoint.common.layout.WrappedLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.idp.EntityInGroup;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.access.DeviceCodeRepository;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;

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

	private String deviceCodeValue;
	private OAuthToken parsedToken;
	private OAuthASProperties config;

	@Autowired
	DeviceSignInView(MessageSource msg, OAuthEndpointsCoordinator coordinator,
			DeviceCodeRepository deviceCodeRepository, IdPEngine idPEngine, AttributeHandlerRegistry handlersRegistry,
			IdentityTypeSupport idTypeSupport, AttributeTypeSupport aTypeSupport,
			VaadinWebLogoutHandler authnProcessor, OAuthRequestValidatorFactory requestValidatorFactory)
	{
		this.msg = msg;
		this.coordinator = coordinator;
		this.deviceCodeRepository = deviceCodeRepository;
		this.idpEngine = new OAuthIdPEngine(idPEngine);
		this.handlersRegistry = handlersRegistry;
		this.idTypeSupport = idTypeSupport;
		this.aTypeSupport = aTypeSupport;
		this.authnProcessor = authnProcessor;
		this.requestValidatorFactory = requestValidatorFactory;
		showCodeEntryForm();
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
		if (userCode == null || userCode.isBlank())
			showCodeEntryForm();
		else
			tryResolve(userCode, true);
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
			parsedToken.setDeviceCodeStatus(DeviceCodeStatus.DENIED);
			if (!updateRecord())
				return;
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
		Optional<Token> tokenOpt = deviceCodeRepository.findByUserCode(userCode);
		if (tokenOpt.isEmpty())
		{
			showError(msg.getMessage("DeviceSignIn.invalidCode"));
			return;
		}
		Token token = tokenOpt.get();
		OAuthToken parsed = BaseOAuthResource.parseInternalToken(token);

		if (token.getExpires() != null && token.getExpires().before(new Date()))
		{
			showError(msg.getMessage("DeviceSignIn.expiredCode"));
			return;
		}
		if (parsed.getDeviceCodeStatus() != DeviceCodeStatus.PENDING)
		{
			showError(msg.getMessage("DeviceSignIn.alreadyProcessed"));
			return;
		}

		Optional<OAuthASProperties> configOpt = coordinator.getDeviceSignInConfig(parsed.getIssuerUri());
		if (configOpt.isEmpty() || !configOpt.get().isDeviceGrantEnabled())
		{
			showError(msg.getMessage("DeviceSignIn.disabled"));
			return;
		}

		this.deviceCodeValue = token.getValue();
		this.parsedToken = parsed;
		this.config = configOpt.get();

		if (confirmCodeStep)
			showConfirmCodeForm(parsed.getUserCode());
		else
			showConsentScreen();
	}

	private void showConfirmCodeForm(String userCode)
	{
		VerticalLayout layout = new VerticalLayout();
		layout.setAlignItems(Alignment.CENTER);
		H2 title = new H2(msg.getMessage("DeviceSignIn.confirmCodeTitle"));
		Span description = new Span(msg.getMessage("DeviceSignIn.confirmCodeDescription"));

		Span code = new Span(userCode);
		code.getStyle().set("font-family", "monospace").set("font-size", "1.5em").set("font-weight", "bold");

		Button cancel = new Button(msg.getMessage("cancel"), e -> onCancel());
		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		Button confirm = new Button(msg.getMessage("DeviceSignIn.confirmCodeSubmit"), e -> showConsentScreen());
		confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout buttons = new HorizontalLayout(cancel, confirm);

		Span warning = new Span(msg.getMessage("DeviceSignIn.confirmCodeWarning"));

		layout.add(title, description, code, buttons, warning);
		getContent().removeAll();
		getContent().add(layout);
	}

	private void showConsentScreen()
	{
		try
		{
			OAuthRequestValidator requestValidator = requestValidatorFactory.getOAuthRequestValidator(config);
			Map<String, AttributeExt> clientAttributes = requestValidator
					.getAttributesNoAuthZ(new EntityParam(parsedToken.getClientId()));

			String usersGroup = getUsersGroup(clientAttributes);
			EntityInGroup requesterEntity = new EntityInGroup(config.getValue(OAuthASProperties.CLIENTS_GROUP),
					new EntityParam(parsedToken.getClientId()));
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();

			TranslationResult translationResult = idpEngine.getUserInfoUnsafe(loginSession.getEntityId(),
					parsedToken.getClientUsername(), Optional.of(requesterEntity), usersGroup,
					config.getOutputTranslationProfile(), GrantFlow.deviceCode.toString(), config, null);

			IdentityParam identity = idpEngine.getIdentity(translationResult, config.getSubjectIdentityType());

			Set<String> requestedAttributes = new HashSet<>();
			for (RequestedOAuthScope si : parsedToken.getEffectiveScope())
				requestedAttributes.addAll(si.scopeDefinition().attributes());
			Set<DynamicAttribute> attributes = OAuthProcessor.filterAttributes(translationResult, requestedAttributes);

			Image clientLogo = buildClientLogo(clientAttributes);
			String clientName = parsedToken.getClientName() != null ? parsedToken.getClientName()
					: parsedToken.getClientUsername();

			DeviceSignInConsentScreen consentScreen = new DeviceSignInConsentScreen(msg, handlersRegistry,
					authnProcessor, idTypeSupport, clientName, clientLogo, parsedToken.getEffectiveScope(), identity,
					attributes, DeviceSignInWebEndpoint.SERVLET_PATH, this::onDeny, this::onAccept);
			getContent().removeAll();
			getContent().add(consentScreen);
		} catch (Exception e)
		{
			log.error("Error while preparing the device sign-in consent screen", e);
			showError(msg.getMessage("DeviceSignIn.internalError"));
		}
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

		parsedToken.setSubject(identity.getValue());
		parsedToken.setUserInfo(userInfo.toJSONObject().toJSONString());
		parsedToken.setAuthenticationTime(loginSession.getAuthenticationTime());
		parsedToken.setSubjectEntityId(loginSession.getEntityId());
		parsedToken.setTokenValidity(config.getAccessTokenValidity());
		parsedToken.setMaxExtendedValidity(config.getMaxExtendedAccessTokenValidity());
		parsedToken.setAudience(List.of(parsedToken.getClientUsername()));
		parsedToken.setDeviceCodeStatus(DeviceCodeStatus.APPROVED);

		if (!updateRecord())
			return;

		showCompleted(true);
	}

	private void onDeny()
	{
		parsedToken.setDeviceCodeStatus(DeviceCodeStatus.DENIED);
		updateRecord();
		showCompleted(false);
	}

	private boolean updateRecord()
	{
		try
		{
			deviceCodeRepository.update(deviceCodeValue, parsedToken, null);
			return true;
		} catch (JsonProcessingException e)
		{
			log.error("Can not update the device code record", e);
			showError(msg.getMessage("DeviceSignIn.internalError"));
			return false;
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
