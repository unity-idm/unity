/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.devicesignin;

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
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
import io.imunity.vaadin.endpoint.common.forms.components.WorkflowCompletedComponent;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.layout.WrappedLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClientHelper.ActiveValueSelectionConfig;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.devicesignin.DeviceCodeTransitionService.TransitionResult;
import pl.edu.icm.unity.oauth.as.devicesignin.DeviceSignInWorkflowPreparer.ClientInfo;
import pl.edu.icm.unity.oauth.as.devicesignin.DeviceSignInWorkflowPreparer.ConsentPresentation;
import pl.edu.icm.unity.oauth.as.devicesignin.DeviceSignInWorkflowPreparer.ResolveError;
import pl.edu.icm.unity.oauth.as.devicesignin.DeviceSignInWorkflowPreparer.ResolveErrorReason;
import pl.edu.icm.unity.oauth.as.devicesignin.DeviceSignInWorkflowPreparer.ResolveResult;
import pl.edu.icm.unity.oauth.as.devicesignin.DeviceSignInWorkflowPreparer.Resolved;

/**
 * RFC 8628 §3.3: device sign-in view. Reads an optional {@code ?user_code=} query parameter; if
 * absent, shows a code-entry form. Once a pending device code record is identified, shows a consent
 * screen and, on confirm/deny, updates the record so that the device's poll on {@code /token} can
 * pick up the result. Always requires an authenticated user ({@code @PermitAll} + the standard
 * authentication filter from {@link SecureVaadin2XEndpoint}).
 * <p>
 * This class is deliberately kept to view + workflow-orchestration concerns only: resolving a code
 * and preparing consent data is {@link DeviceSignInWorkflowPreparer}'s job, and mutating/persisting
 * a decision is {@link DeviceCodeTransitionService}'s.
 */
@PermitAll
@Route(value = DeviceSignInWebEndpoint.SERVLET_PATH, layout = WrappedLayout.class)
class DeviceSignInView extends UnityViewComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, DeviceSignInView.class);

	private final MessageSource msg;
	private final DeviceSignInWorkflowPreparer workflowPreparer;
	private final DeviceCodeTransitionService transitionService;
	private final AttributeHandlerRegistry handlersRegistry;
	private final IdentityTypeSupport idTypeSupport;
	private final VaadinWebLogoutHandler authnProcessor;
	private final PolicyAgreementManagement policyAgreementsMan;
	private final PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder;
	private final NotificationPresenter notificationPresenter;
	private final EnquiresDialogLauncher enquiresDialogLauncher;

	private String deviceCodeValue;
	private DeviceCodeToken parsedToken;
	private OAuthASProperties config;
	private List<DynamicAttribute> activeValueSelectionFilteredAttributes;
	private TextField codeEntryField;

	@Autowired
	DeviceSignInView(MessageSource msg, DeviceSignInWorkflowPreparer workflowPreparer,
			DeviceCodeTransitionService transitionService, AttributeHandlerRegistry handlersRegistry,
			IdentityTypeSupport idTypeSupport, VaadinWebLogoutHandler authnProcessor,
			PolicyAgreementManagement policyAgreementsMan,
			PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder,
			NotificationPresenter notificationPresenter, EnquiresDialogLauncher enquiresDialogLauncher)
	{
		this.msg = msg;
		this.workflowPreparer = workflowPreparer;
		this.transitionService = transitionService;
		this.handlersRegistry = handlersRegistry;
		this.idTypeSupport = idTypeSupport;
		this.authnProcessor = authnProcessor;
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

	/**
	 * Deliberately shares its CSS hook classes and card layout with {@link #showConfirmCodeForm} /
	 * {@link DeviceSignInConsentScreen} for a visually consistent flow, even though the client isn't
	 * known yet at this step (no code has been resolved), so no logo/name header is shown here.
	 */
	private void showCodeEntryForm()
	{
		VerticalLayout layout = new VerticalLayout();
		layout.setMargin(false);
		layout.setSpacing(false);
		layout.setAlignItems(Alignment.CENTER);

		VerticalLayout contents = new VerticalLayout();
		contents.addClassName("u-consentMainColumn");
		contents.setAlignItems(Alignment.CENTER);
		contents.addClassName(CssClassNames.DEVICE_SIGNIN_CARD.getName());
		layout.add(contents);

		contents.add(new H2(msg.getMessage("DeviceSignIn.connectDeviceTitle")));
		contents.add(new Span(msg.getMessage("DeviceSignIn.connectDeviceDescription")));

		Div codePanel = new Div();
		codePanel.setClassName("u-consent-screen");
		contents.add(codePanel);
		VerticalLayout codeGroup = new VerticalLayout();
		codeGroup.setAlignItems(Alignment.CENTER);
		codePanel.add(codeGroup);
		Span codeLabel = new Span(msg.getMessage("DeviceSignIn.enterCode"));
		TextField codeField = new TextField();
		codeGroup.add(codeLabel, codeField);
		this.codeEntryField = codeField;

		Span codeHint = new Span(msg.getMessage("DeviceSignIn.codeHint"));
		codeHint.addClassName(CssClassNames.HINT_TEXT.getName());
		contents.add(codeHint);
		contents.add(new Span(msg.getMessage("DeviceSignIn.codeWarning1")));
		contents.add(new Span(msg.getMessage("DeviceSignIn.codeWarning2")));

		Button cancel = new Button(msg.getMessage("cancel"), e -> onCancel());
		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		Button submit = new Button(msg.getMessage("DeviceSignIn.submit"), e -> tryResolve(codeField.getValue(), false));
		submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		submit.addClickShortcut(Key.ENTER);
		HorizontalLayout buttons = new HorizontalLayout(cancel, submit);
		buttons.setAlignItems(Alignment.CENTER);
		contents.add(buttons);

		getContent().removeAll();
		getContent().add(layout);
	}

	private void onCancel()
	{
		if (parsedToken != null)
		{
			TransitionResult result = transitionService.reject(deviceCodeValue);
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
		long entityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		ResolveResult result = workflowPreparer.resolve(userCode, entityId);
		switch (result)
		{
		case Resolved resolved ->
		{
			this.deviceCodeValue = resolved.deviceCodeValue();
			this.parsedToken = resolved.parsedToken();
			this.config = resolved.config();
			this.activeValueSelectionFilteredAttributes = null;
			if (confirmCodeStep)
				prepareAndShowConfirmCodeForm();
			else
				startConsentFlow();
		}
		case ResolveError error ->
		{
			// a wrong code typed on the code-entry form is by far the most common error here (a
			// typo), so it stays on that same form instead of navigating to a full error page; the
			// URL-provided code path (confirmCodeStep) never had a form to stay on, so it keeps the
			// full-page error, as do the other, rarer reasons on this form
			if (!confirmCodeStep && error.reason() == ResolveErrorReason.INVALID_CODE)
				rejectInvalidManualCode();
			else
				showError(msg.getMessage(resolveErrorMessageKey(error)));
		}
		}
	}

	/**
	 * QA request: on a wrong manually-entered code, don't navigate away - just clear the field and
	 * pop up an error notification. The 3s delay is deliberate and blocks this view's UI for its
	 * duration (the click is a single synchronous server round-trip): server-side throttling
	 * ({@link DeviceCodeVerificationThrottle}) already caps the number of guesses, but this adds a
	 * per-guess minimum latency so an automated guesser can't submit codes back-to-back.
	 */
	private void rejectInvalidManualCode()
	{
		try
		{
			Thread.sleep(3000);
		} catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		if (codeEntryField != null)
			codeEntryField.clear();
		notificationPresenter.showError(msg.getMessage("DeviceSignIn.invalidCode"), "");
	}

	private String resolveErrorMessageKey(ResolveError error)
	{
		return switch (error.reason())
		{
		case INVALID_CODE -> "DeviceSignIn.invalidCode";
		case TOO_MANY_ATTEMPTS -> "DeviceSignIn.tooManyAttempts";
		case EXPIRED_CODE -> "DeviceSignIn.expiredCode";
		case ALREADY_PROCESSED -> "DeviceSignIn.alreadyProcessed";
		case DISABLED -> "DeviceSignIn.disabled";
		};
	}

	private void prepareAndShowConfirmCodeForm()
	{
		try
		{
			ClientInfo clientInfo = workflowPreparer.resolveClientInfo(parsedToken.getOauthToken(), config);
			showConfirmCodeForm(parsedToken.getUserCode(), clientInfo.clientName(), clientInfo.clientLogo());
		} catch (Exception e)
		{
			log.error("Error while preparing the device sign-in confirm code screen", e);
			showError(msg.getMessage("DeviceSignIn.internalError"));
		}
	}

	/**
	 * Deliberately mirrors {@link DeviceSignInConsentScreen}'s structure (same CSS hook classes,
	 * client logo+name header) so that the transition into the consent screen right after this one
	 * changes only the panel content, not the overall layout.
	 */
	private void showConfirmCodeForm(String userCode, String clientName, Image clientLogo)
	{
		VerticalLayout layout = new VerticalLayout();
		layout.setMargin(false);
		layout.setSpacing(false);
		layout.setAlignItems(Alignment.CENTER);

		VerticalLayout contents = new VerticalLayout();
		contents.addClassName("u-consentMainColumn");
		contents.setAlignItems(Alignment.CENTER);
		contents.addClassName(CssClassNames.DEVICE_SIGNIN_CARD.getName());
		layout.add(contents);

		if (clientLogo != null)
			contents.add(clientLogo);
		contents.add(new H2(msg.getMessage("DeviceSignIn.confirmCodeTitle")));
		contents.add(new Span(msg.getMessage("DeviceSignIn.confirmCodeDescription", clientName)));

		Div codePanel = new Div();
		codePanel.setClassName("u-consent-screen");
		contents.add(codePanel);
		VerticalLayout codeLayout = new VerticalLayout();
		codeLayout.setAlignItems(Alignment.CENTER);
		codePanel.add(codeLayout);
		Span code = new Span(userCode);
		code.addClassName(CssClassNames.DEVICE_CODE.getName());
		codeLayout.add(code);

		contents.add(new Span(msg.getMessage("DeviceSignIn.confirmCodeWarning")));

		Button cancel = new Button(msg.getMessage("cancel"), e -> onCancel());
		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		Button confirm = new Button(msg.getMessage("DeviceSignIn.confirmCodeSubmit"), e -> startConsentFlow());
		confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout buttons = new HorizontalLayout(cancel, confirm);
		buttons.setAlignItems(Alignment.CENTER);
		contents.add(buttons);

		getContent().removeAll();
		getContent().add(layout);
	}

	private void startConsentFlow()
	{
		long entityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		List<PolicyAgreementConfiguration> toPresent = workflowPreparer.filterAgreementsToPresent(config, entityId);
		if (!toPresent.isEmpty())
			showPolicyAgreementsScreen(toPresent);
		else
			prepareAndShowConsent();
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
			long entityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
			ConsentPresentation presentation = workflowPreparer.prepareConsent(parsedToken, config, entityId);

			if (presentation.activeValueSelectionConfig().isPresent())
				showActiveValueSelectionScreen(presentation.activeValueSelectionConfig().get(), presentation.identity(),
						presentation.clientName(), presentation.clientLogo());
			else
				buildAndShowConsentScreen(presentation.identity(), presentation.clientName(), presentation.clientLogo(),
						presentation.attributes(), null);
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

	private void onAccept(IdentityParam identity, Collection<DynamicAttribute> attributes)
	{
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		UserInfo userInfo = OAuthProcessor.prepareUserInfoClaimSet(identity.getValue(), attributes);

		TransitionResult result = transitionService.approve(deviceCodeValue, identity, userInfo,
				activeValueSelectionFilteredAttributes, loginSession.getEntityId(),
				loginSession.getAuthenticationTime(), config);

		switch (result)
		{
		case APPLIED -> showCompleted(true);
		case ALREADY_PROCESSED -> showError(msg.getMessage("DeviceSignIn.alreadyProcessed"));
		case FAILED -> showError(msg.getMessage("DeviceSignIn.internalError"));
		}
	}

	private void onDeny()
	{
		TransitionResult result = transitionService.reject(deviceCodeValue);
		switch (result)
		{
		case APPLIED -> showCompleted(false);
		case ALREADY_PROCESSED -> showError(msg.getMessage("DeviceSignIn.alreadyProcessed"));
		case FAILED -> showError(msg.getMessage("DeviceSignIn.internalError"));
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
