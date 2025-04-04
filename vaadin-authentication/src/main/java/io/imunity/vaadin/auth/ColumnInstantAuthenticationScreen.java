/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import static io.imunity.vaadin.endpoint.common.VaadinEndpointProperties.AUTHN_SHOW_CANCEL;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.CancelHandler;
import io.imunity.vaadin.endpoint.common.LocaleChoiceComponent;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import io.imunity.vaadin.endpoint.common.VaadinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAccessCounter;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

/**
 * Organizes authentication options in columns, making them instantly usable.
 */
public class ColumnInstantAuthenticationScreen extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ColumnInstantAuthenticationScreen.class);
	protected final MessageSource msg;
	private final VaadinLogoImageLoader imageAccessService;
	private final VaadinEndpointProperties config;
	protected final ResolvedEndpoint endpointDescription;
	private final Runnable registrationLayoutLauncher;
	private final boolean enableRegistration;
	private final CancelHandler cancelHandler;

	private final EntityManagement idsMan;
	private final ExecutorsService execService;
	private final Function<UnknownRemotePrincipalResult, Dialog> unknownUserDialogProvider;
	private final Optional<LocaleChoiceComponent> localeChoice;
	private final List<AuthenticationFlow> flows;

	protected final InteractiveAuthenticationProcessor interactiveAuthnProcessor;

	private AuthenticationOptionsHandler authnOptionsHandler;
	private FirstFactorAuthNPanel authNPanelInProgress;
	private Checkbox rememberMe;
	private AuthnOptionsColumns authNColumns;
	private VerticalLayout secondFactorHolder;
	private HorizontalLayout rememberMeComponent;
	private TopHeaderComponent topHeader;
	private HorizontalLayout cancelComponent;
	private final CredentialResetLauncher credentialResetLauncher;
	protected final NotificationPresenter notificationPresenter;

	protected ColumnInstantAuthenticationScreen(MessageSource msg, VaadinLogoImageLoader imageAccessService,
			VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			CredentialResetLauncher credentialResetLauncher,
			Runnable registrationLayoutLauncher, CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, boolean enableRegistration,
			Function<UnknownRemotePrincipalResult, Dialog> unknownUserDialogProvider,
			Optional<LocaleChoiceComponent> localeChoice,
			List<AuthenticationFlow> flows,
			InteractiveAuthenticationProcessor interactiveAuthnProcessor,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.config = config;
		this.endpointDescription = endpointDescription;
		this.credentialResetLauncher = credentialResetLauncher;
		this.registrationLayoutLauncher = registrationLayoutLauncher;
		this.cancelHandler = cancelHandler;
		this.idsMan = idsMan;
		this.execService = execService;
		this.enableRegistration = enableRegistration;
		this.unknownUserDialogProvider = unknownUserDialogProvider;
		this.localeChoice = localeChoice;
		this.flows = flows;
		this.imageAccessService = imageAccessService;
		this.interactiveAuthnProcessor = interactiveAuthnProcessor;
		this.notificationPresenter = notificationPresenter;
	}

	public static ColumnInstantAuthenticationScreen getInstance(MessageSource msg,
			VaadinLogoImageLoader imageAccessService,
			VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			CredentialResetLauncher credentialResetLauncher,
			Runnable registrationLayoutLauncher, CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, boolean enableRegistration,
			Function<UnknownRemotePrincipalResult, Dialog> unknownUserDialogProvider,
			Optional<LocaleChoiceComponent> localeChoice,
			List<AuthenticationFlow> flows,
			InteractiveAuthenticationProcessor interactiveAuthnProcessor,
			NotificationPresenter notificationPresenter)
	{
		ColumnInstantAuthenticationScreen instance = new ColumnInstantAuthenticationScreen(msg,
				imageAccessService, config, endpointDescription,
				credentialResetLauncher, registrationLayoutLauncher, cancelHandler, idsMan, execService,
				enableRegistration, unknownUserDialogProvider, localeChoice, flows,
				interactiveAuthnProcessor, notificationPresenter);
		instance.init();
		return instance;
	}

	public void reset()
	{
		switchBackToPrimaryAuthentication();
	}

	protected final void init()
	{
		log.debug("Authn screen init");
		this.authnOptionsHandler = new AuthenticationOptionsHandler(flows, endpointDescription.getName(),
				endpointDescription.getRealm(), endpointDescription.getEndpoint().getContextAddress());

		topHeader = new TopHeaderComponent(localeChoice, enableRegistration, config,
				registrationLayoutLauncher, msg);
		add(topHeader);

		Component authnOptionsComponent = getAuthenticationComponent();
		add(authnOptionsComponent);
		setWidthFull();
		setMargin(false);
		setPadding(false);

		log.debug("Authn screen init finished loading authenticators");
	}

	/**
	 * @return main authentication: logo, title, columns with authentication options
	 */
	private Component getAuthenticationComponent()
	{
		VerticalLayout authenticationMainLayout = new VerticalLayout();
		authenticationMainLayout.setMargin(false);
		authenticationMainLayout.setPadding(false);
		authenticationMainLayout.getStyle().set("gap", "0");

		String logoUri = config.getAuthnLogo();
		Optional<LocalOrRemoteResource> image = imageAccessService.loadImageFromUri(logoUri);
		if (image.isPresent())
		{
			image.get().addClassName("u-authn-logo");
			image.get().getStyle().set("max-height", "5rem");
			VerticalLayout verticalLayout = new VerticalLayout(image.get());
			verticalLayout.setAlignItems(Alignment.CENTER);
			authenticationMainLayout.add(verticalLayout);
		}

		Component title = getTitleComponent();
		if (title != null)
		{
			authenticationMainLayout.add(title);
			authenticationMainLayout.setAlignItems(Alignment.CENTER);
		}

		authNColumns = new AuthnOptionsColumns(config, msg,
				authnOptionsHandler, enableRegistration, new AuthnPanelFactoryImpl(),
				registrationLayoutLauncher);

		authenticationMainLayout.add(authNColumns);
		authenticationMainLayout.setAlignItems(Alignment.CENTER);

		secondFactorHolder = new VerticalLayout();
		secondFactorHolder.setMargin(false);
		secondFactorHolder.setPadding(false);
		secondFactorHolder.setVisible(false);
		authenticationMainLayout.add(secondFactorHolder);

		AuthenticationRealm realm = endpointDescription.getRealm();
		rememberMeComponent = getRememberMeComponent(realm);
		rememberMeComponent.setVisible(
				getRememberMePolicy().equals(RememberMePolicy.allowForWholeAuthn));
		authenticationMainLayout.add(rememberMeComponent);

		if (cancelHandler != null && config.getBooleanValue(AUTHN_SHOW_CANCEL))
		{
			cancelComponent = getCancelComponent();
			authenticationMainLayout.add(cancelComponent);
		}

		return authenticationMainLayout;
	}

	private HorizontalLayout getCancelComponent()
	{
		LinkButton cancel = new LinkButton(msg.getMessage("AuthenticationUI.cancelAuthentication"), e ->
		{
			if (authNPanelInProgress != null)
				authNPanelInProgress.cancel();
			cancelHandler.onCancel();
		});
		HorizontalLayout bottomWrapper = new HorizontalLayout();
		bottomWrapper.setMargin(true);
		bottomWrapper.setWidthFull();
		bottomWrapper.add(cancel);
		bottomWrapper.setJustifyContentMode(JustifyContentMode.CENTER);
		return bottomWrapper;
	}

	protected HorizontalLayout getRememberMeComponent(AuthenticationRealm realm)
	{
		HorizontalLayout bottomWrapper = new HorizontalLayout();
		bottomWrapper.setWidthFull();
		rememberMe = new Checkbox(msg.getMessage("AuthenticationUI.rememberMe",
				realm.getAllowForRememberMeDays()));
		rememberMe.addClassName("u-authn-rememberMe");
		bottomWrapper.add(rememberMe);
		bottomWrapper.setJustifyContentMode(JustifyContentMode.CENTER);
		bottomWrapper.setMargin(true);
		bottomWrapper.setPadding(true);
		bottomWrapper.setSpacing(true);
		return bottomWrapper;
	}

	private Component getTitleComponent()
	{
		String mainTitle = getTitle();
		if (mainTitle != null)
		{
			H2 mainTitleLabel = new H2(mainTitle);
			mainTitleLabel.addClassName("u-authn-title");
			mainTitleLabel.getStyle().set("margin-top", "0.5em");
			mainTitleLabel.getStyle().set("margin-bottom", "2em");
			return mainTitleLabel;
		}
		return null;
	}
	
	public String getTitle()
	{
		String configuredMainTitle = config.getLocalizedValue(VaadinEndpointProperties.AUTHN_TITLE, msg.getLocale());
		String mainTitle = null;
		String serviceName = endpointDescription.getEndpoint().getConfiguration().getDisplayedName().getValue(msg);
		if (serviceName == null)
			serviceName = endpointDescription.getEndpoint().getName();

		if (configuredMainTitle != null && !configuredMainTitle.isEmpty())
		{
			mainTitle = String.format(configuredMainTitle, serviceName);
		} else 
		{
			mainTitle = msg.getMessage("AuthenticationUI.login", serviceName);
		}
		return mainTitle;
	}

	private FirstFactorAuthNPanel buildBaseAuthenticationOptionWidget(AuthNOption authnOption, boolean gridCompatible)
	{
		AuthenticationOptionKey optionId = new AuthenticationOptionKey(authnOption.authenticator.getAuthenticatorId(),
				authnOption.authenticatorUI.getId());
		FirstFactorAuthNPanel authNPanel = new FirstFactorAuthNPanel(
				cancelHandler, unknownUserDialogProvider, gridCompatible,
				authnOption.authenticatorUI, optionId);
		AuthenticationStepContext stepContext = new AuthenticationStepContext(endpointDescription.getRealm(),
				authnOption.flow,
				optionId,
				FactorOrder.FIRST,
				endpointDescription.getEndpoint().getContextAddress(), SigInInProgressContextService.getVaadinContext());
		VaadinAuthentication.AuthenticationCallback controller = createFirstFactorAuthnCallback(optionId, authNPanel,
				stepContext);
		authnOption.authenticatorUI.setAuthenticationCallback(controller);
		authnOption.authenticatorUI.setCredentialResetLauncher(credentialResetLauncher);
		return authNPanel;
	}

	protected VaadinAuthentication.AuthenticationCallback createFirstFactorAuthnCallback(
			AuthenticationOptionKey optionId,
			FirstFactorAuthNPanel authNPanel, AuthenticationStepContext stepContext)
	{
		return new FirstFactorAuthNResultCallback(
				msg, interactiveAuthnProcessor,
				stepContext,
				this::isSetRememberMe,
				new PrimaryAuthenticationListenerImpl(optionId.toStringEncodedKey(), authNPanel),
				authNPanel,
				notificationPresenter);
	}

	private SecondFactorAuthNPanel build2ndFactorAuthenticationOptionWidget(
			VaadinAuthentication.VaadinAuthenticationUI secondaryUI,
			PartialAuthnState partialAuthnState)
	{
		AuthenticationOptionKey optionId = new AuthenticationOptionKey(
				partialAuthnState.getSecondaryAuthenticator().getAuthenticatorId(),
				secondaryUI.getId());
		SecondFactorAuthNPanel authNPanel = new SecondFactorAuthNPanel(msg, idsMan,
				secondaryUI, partialAuthnState,
				optionId, this::switchBackToPrimaryAuthentication);
		AuthenticationStepContext stepContext = new AuthenticationStepContext(endpointDescription.getRealm(),
				partialAuthnState.getAuthenticationFlow(),
				authNPanel.getAuthenticationOptionId(), FactorOrder.SECOND, null, SigInInProgressContextService.getVaadinContext());
		VaadinAuthentication.AuthenticationCallback controller = createSecondFactorAuthnCallback(optionId,
				authNPanel, stepContext, partialAuthnState);
		secondaryUI.setAuthenticationCallback(controller);
		secondaryUI.setCredentialResetLauncher(credentialResetLauncher);
		return authNPanel;
	}

	protected VaadinAuthentication.AuthenticationCallback createSecondFactorAuthnCallback(
			AuthenticationOptionKey optionId,
			SecondFactorAuthNPanel authNPanel, AuthenticationStepContext stepContext,
			PartialAuthnState partialAuthnState)
	{
		return new SecondFactorAuthNResultCallback(msg,
				interactiveAuthnProcessor, stepContext,
				new SecondaryAuthenticationListenerImpl(), this::isSetRememberMe,
				partialAuthnState, authNPanel, notificationPresenter);
	}

	private boolean isSetRememberMe()
	{
		return rememberMe != null && rememberMe.getValue();
	}

	public void initializeAfterReturnFromExternalAuthn(PostAuthenticationStepDecision postAuthnStepDecision)
	{
		RedirectedAuthnResultProcessor remoteFirstFactorResultProcessor =
				new RedirectedAuthnResultProcessor(msg, execService,
						unknownUserDialogProvider,
						this::switchToSecondaryAuthentication,
						notificationPresenter);
		remoteFirstFactorResultProcessor.onCompletedAuthentication(postAuthnStepDecision);
	}

	void showWaitScreenIfNeeded(String clientIp)
	{
		UnsuccessfulAccessCounter counter = VaadinWebLogoutHandler.getLoginCounter();
		if (counter.getRemainingBlockedTime(clientIp) > 0)
			new AccessBlockedDialog(msg, execService).show();
	}

	private void onAbortedAuthentication()
	{
		authNColumns.enableAll();
		enableSharedWidgets(true);
		showWaitScreenIfNeeded(HTTPRequestContext.getCurrent().getClientIP());
		authNPanelInProgress = null;
	}

	private void switchToSecondaryAuthentication(PartialAuthnState partialState)
	{
		enableSharedWidgets(true);
		authNPanelInProgress = null;
		VaadinAuthentication secondaryAuthn = (VaadinAuthentication) partialState.getSecondaryAuthenticator();

		AuthenticatorStepContext context = new AuthenticatorStepContext(endpointDescription.getRealm(),
				partialState.getAuthenticationFlow(), null, FactorOrder.SECOND);
		Collection<VaadinAuthentication.VaadinAuthenticationUI> secondaryAuthnUIs = secondaryAuthn.createUIInstance(
				VaadinAuthentication.Context.LOGIN,
				context);
		if (secondaryAuthnUIs.size() > 1)
		{
			log.warn("Configuration error: the authenticator configured as the second "
					+ "factor " + secondaryAuthn.getAuthenticatorId() +
					" provides multiple authentication possibilities. "
					+ "This is unsupported currently, "
					+ "use this authenticator as the first factor only. "
					+ "The first possibility will be used, "
					+ "but in most cases it is not what you want.");
		}
		VaadinAuthentication.VaadinAuthenticationUI secondaryUI = secondaryAuthnUIs.iterator().next();

		authNColumns.setVisible(false);

		SecondFactorAuthNPanel authNPanel = build2ndFactorAuthenticationOptionWidget(secondaryUI, partialState);
		AuthnOptionsColumn wrapping2ndFColumn = new AuthnOptionsColumn(null,
				VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH);
		wrapping2ndFColumn.addOptions(Lists.newArrayList(
				new AuthnOptionsColumn.ComponentWithId("", authNPanel, 1, i -> Optional.empty())));
		secondFactorHolder.removeAll();
		Span mfaInfo = new Span(msg.getMessage("AuthenticationUI.mfaRequired"));
		wrapping2ndFColumn.focusFirst();
		secondFactorHolder.add(mfaInfo);
		secondFactorHolder.add(wrapping2ndFColumn);
		secondFactorHolder.setAlignItems(Alignment.CENTER);
		secondFactorHolder.setVisible(true);
		rememberMeComponent.setVisible(
				getRememberMePolicy().equals(RememberMePolicy.allowFor2ndFactor));
	}

	protected RememberMePolicy getRememberMePolicy()
	{
		AuthenticationRealm realm = endpointDescription.getRealm();
		return realm.getRememberMePolicy();
	}

	private void switchBackToPrimaryAuthentication()
	{
		authNColumns.setVisible(true);
		authNColumns.enableAll();
		authNColumns.focusFirst();
		enableSharedWidgets(true);
		secondFactorHolder.removeAll();
		secondFactorHolder.setVisible(false);
		rememberMeComponent.setVisible(
				getRememberMePolicy().equals(RememberMePolicy.allowForWholeAuthn));

	}

	private void enableSharedWidgets(boolean enable)
	{
		rememberMeComponent.setEnabled(enable);
		if (cancelComponent != null)
			cancelComponent.setEnabled(enable);
		topHeader.setEnabled(enable);
	}

	private void onCompletedAuthentication()
	{
		authNPanelInProgress = null;
	}

	private class AuthnPanelFactoryImpl implements AuthNPanelFactory
	{
		@Override
		public FirstFactorAuthNPanel createRegularAuthnPanel(AuthNOption authnOption)
		{
			return buildBaseAuthenticationOptionWidget(authnOption, false);
		}

		@Override
		public FirstFactorAuthNPanel createGridCompatibleAuthnPanel(AuthNOption authnOption)
		{
			return buildBaseAuthenticationOptionWidget(authnOption, true);
		}
	}

	public class PrimaryAuthenticationListenerImpl implements FirstFactorAuthenticationListener
	{
		private final String optionId;
		private final FirstFactorAuthNPanel authNPanel;

		public PrimaryAuthenticationListenerImpl(String selectedComponentId, FirstFactorAuthNPanel authNPanel)
		{
			this.optionId = selectedComponentId;
			this.authNPanel = authNPanel;
		}

		@Override
		public void authenticationStarted()
		{
			authNPanelInProgress = authNPanel;
			authNColumns.disableAllExcept(optionId);
			enableSharedWidgets(false);
		}

		@Override
		public void authenticationAborted()
		{
			onAbortedAuthentication();
		}

		@Override
		public void switchTo2ndFactor(PartialAuthnState partialState)
		{
			switchToSecondaryAuthentication(partialState);
		}

		@Override
		public void authenticationCompleted()
		{
			onCompletedAuthentication();
		}
	}

	public class SecondaryAuthenticationListenerImpl implements SecondFactorAuthenticationListener
	{
		@Override
		public void switchBackToFirstFactor()
		{
			switchBackToPrimaryAuthentication();
		}

		@Override
		public void authenticationStarted()
		{
			enableSharedWidgets(false);
		}

		@Override
		public void authenticationAborted()
		{
			onAbortedAuthentication();
		}

		@Override
		public void authenticationCompleted()
		{
			onCompletedAuthentication();
		}
	}


	/**
	 * Used be this component to be informed about changes in authn process
	 */
	public interface FirstFactorAuthenticationListener
	{
		void authenticationStarted();

		void authenticationAborted();

		void authenticationCompleted();

		void switchTo2ndFactor(PartialAuthnState partialState);
	}


	/**
	 * Used be this component to be informed about changes in authn process
	 */
	public interface SecondFactorAuthenticationListener
	{
		void authenticationStarted();

		void authenticationAborted();

		void authenticationCompleted();

		void switchBackToFirstFactor();
	}
}
