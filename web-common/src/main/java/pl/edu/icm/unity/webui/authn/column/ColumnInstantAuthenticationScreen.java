/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_SHOW_CANCEL;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
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
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.AuthenticationScreen;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.CredentialResetLauncher;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.UnknownUserDialog;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.Label100;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

/**
 * Organizes authentication options in columns, making them instantly usable.
 * 
 * @author K. Benedyczak
 */
public class ColumnInstantAuthenticationScreen extends CustomComponent implements AuthenticationScreen
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ColumnInstantAuthenticationScreen.class);
	protected final MessageSource msg;
	private final ImageAccessService imageAccessService;
	private final VaadinEndpointProperties config;
	private final ResolvedEndpoint endpointDescription;
	private final Supplier<Boolean> outdatedCredentialDialogLauncher;
	private final Runnable registrationLayoutLauncher;
	private final boolean enableRegistration;
	private final CancelHandler cancelHandler;
	
	private final EntityManagement idsMan;
	private final ExecutorsService execService;
	private final Function<UnknownRemotePrincipalResult, UnknownUserDialog> unknownUserDialogProvider;
	private final LocaleChoiceComponent localeChoice;
	private final List<AuthenticationFlow> flows;

	protected final InteractiveAuthenticationProcessor interactiveAuthnProcessor;
	
	private AuthenticationOptionsHandler authnOptionsHandler;
	private FirstFactorAuthNPanel authNPanelInProgress;
	private CheckBox rememberMe;
	private AuthnOptionsColumns authNColumns;
	private VerticalLayout secondFactorHolder;
	private Component rememberMeComponent;
	private Component topHeader;
	private Component cancelComponent;
	private CredentialResetLauncher credentialResetLauncher;
	
	protected ColumnInstantAuthenticationScreen(MessageSource msg, ImageAccessService imageAccessService, 
			VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			Supplier<Boolean> outdatedCredentialDialogLauncher,
			CredentialResetLauncher credentialResetLauncher,
			Runnable registrationLayoutLauncher, CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, boolean enableRegistration,
			Function<UnknownRemotePrincipalResult, UnknownUserDialog> unknownUserDialogProvider,
			LocaleChoiceComponent localeChoice,
			List<AuthenticationFlow> flows,
			InteractiveAuthenticationProcessor interactiveAuthnProcessor)
	{
		this.msg = msg;
		this.config = config;
		this.endpointDescription = endpointDescription;
		this.outdatedCredentialDialogLauncher = outdatedCredentialDialogLauncher;
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
	}
	
	public static ColumnInstantAuthenticationScreen getInstance(MessageSource msg, ImageAccessService imageAccessService, 
			VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			Supplier<Boolean> outdatedCredentialDialogLauncher,
			CredentialResetLauncher credentialResetLauncher,
			Runnable registrationLayoutLauncher, CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, boolean enableRegistration,
			Function<UnknownRemotePrincipalResult, UnknownUserDialog> unknownUserDialogProvider,
			LocaleChoiceComponent localeChoice,
			List<AuthenticationFlow> flows,
			InteractiveAuthenticationProcessor interactiveAuthnProcessor)
	{
		ColumnInstantAuthenticationScreen instance = new ColumnInstantAuthenticationScreen(msg,
				imageAccessService, config, endpointDescription, outdatedCredentialDialogLauncher,
				credentialResetLauncher, registrationLayoutLauncher, cancelHandler, idsMan, execService,
				enableRegistration, unknownUserDialogProvider, localeChoice, flows,
				interactiveAuthnProcessor);
		instance.init();
		return instance;
	}

	@Override
	public void reset()
	{
		switchBackToPrimaryAuthentication();
	}
	
	protected final void init()
	{
		log.debug("Authn screen init");
		this.authnOptionsHandler = new AuthenticationOptionsHandler(flows, endpointDescription.getName(), 
				endpointDescription.getRealm(), endpointDescription.getEndpoint().getContextAddress());
		
		VerticalLayout topLevelLayout = new VerticalLayout();
		topLevelLayout.setMargin(new MarginInfo(false, true, true, true));
		topLevelLayout.setHeightUndefined();
		setCompositionRoot(topLevelLayout);
		
		topHeader = new TopHeaderComponent(localeChoice, enableRegistration, config, 
				registrationLayoutLauncher, msg);

		topLevelLayout.addComponent(topHeader);
		topLevelLayout.setComponentAlignment(topHeader, Alignment.MIDDLE_RIGHT);
		
		Component authnOptionsComponent = getAuthenticationComponent();
		topLevelLayout.addComponent(authnOptionsComponent);
		topLevelLayout.setComponentAlignment(authnOptionsComponent, Alignment.MIDDLE_CENTER);
		
		log.debug("Authn screen init finished loading authenticators");
		
		if (outdatedCredentialDialogLauncher.get())
		{
			log.info("Launched outdated credential dialog");
			return;
		}
	}
	
	/**
	 * @return main authentication: logo, title, columns with authentication options
	 */
	private Component getAuthenticationComponent()
	{
		VerticalLayout authenticationMainLayout = new VerticalLayout();
		authenticationMainLayout.setMargin(false);
		
		String logoUri = config.getValue(VaadinEndpointProperties.AUTHN_LOGO);
		Optional<Resource> logoRes = imageAccessService.getConfiguredImageResourceFromNullableUri(logoUri);
		if (logoRes.isPresent())
		{
			Image image = new Image(null, logoRes.get());
			image.addStyleName("u-authn-logo");
			authenticationMainLayout.addComponent(image);
			authenticationMainLayout.setComponentAlignment(image, Alignment.TOP_CENTER);
		}
		
		Component title = getTitleComponent();
		if (title != null)
		{
			authenticationMainLayout.addComponent(title);
			authenticationMainLayout.setComponentAlignment(title, Alignment.TOP_CENTER);
		}
		
		authNColumns = new AuthnOptionsColumns(config, msg, 
				authnOptionsHandler, enableRegistration, new AuthnPanelFactoryImpl(), 
				registrationLayoutLauncher);
		
		authenticationMainLayout.addComponent(authNColumns);
		authenticationMainLayout.setComponentAlignment(authNColumns, Alignment.TOP_CENTER);
		
		secondFactorHolder = new VerticalLayout();
		secondFactorHolder.setMargin(false);
		authenticationMainLayout.addComponent(secondFactorHolder);
		authenticationMainLayout.setComponentAlignment(secondFactorHolder, Alignment.TOP_CENTER);
		secondFactorHolder.setVisible(false);
		
		AuthenticationRealm realm = endpointDescription.getRealm();
		rememberMeComponent = getRememberMeComponent(realm);
		rememberMeComponent.setVisible(
				getRememberMePolicy().equals(RememberMePolicy.allowForWholeAuthn));
		authenticationMainLayout.addComponent(rememberMeComponent);
		
		if (cancelHandler != null && config.getBooleanValue(AUTHN_SHOW_CANCEL))
		{
			cancelComponent = getCancelComponent();
			authenticationMainLayout.addComponent(cancelComponent);
		}
		
		return authenticationMainLayout;
	}
	
	private Component getCancelComponent()
	{
		Button cancel = new Button(msg.getMessage("AuthenticationUI.cancelAuthentication"));
		cancel.addStyleName(Styles.vButtonLink.toString());
		cancel.addClickListener(event -> {
			if (authNPanelInProgress != null)
				authNPanelInProgress.cancel();
			cancelHandler.onCancel();
		});
		HorizontalLayout bottomWrapper = new HorizontalLayout();
		bottomWrapper.setMargin(true);
		bottomWrapper.setWidth(100, Unit.PERCENTAGE);
		bottomWrapper.addComponent(cancel);
		bottomWrapper.setComponentAlignment(cancel, Alignment.TOP_CENTER);
		return bottomWrapper;
	}
	
	private Component getRememberMeComponent(AuthenticationRealm realm)
	{
		HorizontalLayout bottomWrapper = new HorizontalLayout();
		bottomWrapper.setMargin(true);
		bottomWrapper.setWidth(100, Unit.PERCENTAGE);
		rememberMe = new CheckBox(msg.getMessage("AuthenticationUI.rememberMe", 
				realm.getAllowForRememberMeDays()));
		rememberMe.addStyleName("u-authn-rememberMe");
		bottomWrapper.addComponent(rememberMe);
		bottomWrapper.setComponentAlignment(rememberMe, Alignment.TOP_CENTER);
		return bottomWrapper;
	}
	
	private Component getTitleComponent()
	{
		String configuredMainTitle = config.getLocalizedValue(VaadinEndpointProperties.AUTHN_TITLE, msg.getLocale());
		String mainTitle = null;
		String serviceName = endpointDescription.getEndpoint().getConfiguration().getDisplayedName().getValue(msg);
		if (serviceName == null)
			serviceName = endpointDescription.getEndpoint().getName();

		if (configuredMainTitle != null && !configuredMainTitle.isEmpty())
		{
			mainTitle = String.format(configuredMainTitle, serviceName);
		} else if (configuredMainTitle == null)
		{
			mainTitle = msg.getMessage("AuthenticationUI.login", serviceName);
		}
		if (mainTitle != null)
		{
			Label mainTitleLabel = new Label100(mainTitle);
			mainTitleLabel.addStyleName("u-authn-title");
			mainTitleLabel.addStyleName(Styles.textCenter.toString());
			return mainTitleLabel;
		}
		return null;
	}
	
	private FirstFactorAuthNPanel buildBaseAuthenticationOptionWidget(AuthNOption authnOption, boolean gridCompatible)
	{
		AuthenticationOptionKey optionId = new AuthenticationOptionKey(authnOption.authenticator.getAuthenticatorId(), 
				authnOption.authenticatorUI.getId());

		FirstFactorAuthNPanel authNPanel = new FirstFactorAuthNPanel(msg, execService, 
				cancelHandler, unknownUserDialogProvider, gridCompatible, 
				authnOption.authenticatorUI, optionId);
		AuthenticationStepContext stepContext = new AuthenticationStepContext(endpointDescription.getRealm(), 
				authnOption.flow, 
				optionId, 
				FactorOrder.FIRST, 
				endpointDescription.getEndpoint().getContextAddress());
		AuthenticationCallback controller = createFirstFactorAuthnCallback(optionId, authNPanel, stepContext);
		authnOption.authenticatorUI.setAuthenticationCallback(controller);
		authnOption.authenticatorUI.setCredentialResetLauncher(credentialResetLauncher);
		return authNPanel;
	}

	protected AuthenticationCallback createFirstFactorAuthnCallback(AuthenticationOptionKey optionId,
			FirstFactorAuthNPanel authNPanel, AuthenticationStepContext stepContext)
	{
		return new FirstFactorAuthNResultCallback(
				msg, interactiveAuthnProcessor, 
				stepContext, 
				this::isSetRememberMe, 
				new PrimaryAuthenticationListenerImpl(optionId.toStringEncodedKey(), authNPanel), 
				authNPanel);
	}

	private SecondFactorAuthNPanel build2ndFactorAuthenticationOptionWidget(VaadinAuthenticationUI secondaryUI, 
			PartialAuthnState partialAuthnState)
	{
		AuthenticationOptionKey optionId = new AuthenticationOptionKey(
				partialAuthnState.getSecondaryAuthenticator().getAuthenticatorId(), 
				secondaryUI.getId());
		SecondFactorAuthNPanel authNPanel = new SecondFactorAuthNPanel(msg, idsMan, execService, 
				secondaryUI, partialAuthnState, 
				optionId, this::switchBackToPrimaryAuthentication);
		AuthenticationStepContext stepContext = new AuthenticationStepContext(endpointDescription.getRealm(), 
				partialAuthnState.getAuthenticationFlow(), 
				authNPanel.getAuthenticationOptionId(), FactorOrder.SECOND, null);
		AuthenticationCallback controller = createSecondFactorAuthnCallback(optionId, 
				authNPanel, stepContext, partialAuthnState);
		secondaryUI.setAuthenticationCallback(controller);
		secondaryUI.setCredentialResetLauncher(credentialResetLauncher);
		return authNPanel;
	}

	protected AuthenticationCallback createSecondFactorAuthnCallback(AuthenticationOptionKey optionId,
			SecondFactorAuthNPanel authNPanel, AuthenticationStepContext stepContext, 
			PartialAuthnState partialAuthnState)
	{
		return new SecondFactorAuthNResultCallback(msg, 
				interactiveAuthnProcessor, stepContext, 
				new SecondaryAuthenticationListenerImpl(), this::isSetRememberMe, 
				partialAuthnState, authNPanel);
	}
	
	private boolean isSetRememberMe()
	{
		return rememberMe != null && rememberMe.getValue();
	}

	@Override
	public void initializeAfterReturnFromExternalAuthn(PostAuthenticationStepDecision postAuthnStepDecision)
	{
		RedirectedAuthnResultProcessor remoteFirstFactorResultProcessor = 
				new RedirectedAuthnResultProcessor(msg, execService, 
						unknownUserDialogProvider,
						this::switchToSecondaryAuthentication);
		remoteFirstFactorResultProcessor.onCompletedAuthentication(postAuthnStepDecision);
	}

	private void onAbortedAuthentication()
	{
		authNColumns.enableAll();
		enableSharedWidgets(true);
		if (authNPanelInProgress != null)
			authNPanelInProgress.showWaitScreenIfNeeded(HTTPRequestContext.getCurrent().getClientIP());
		authNPanelInProgress = null;
	}
	
	private void switchToSecondaryAuthentication(PartialAuthnState partialState)
	{
		enableSharedWidgets(true);
		authNPanelInProgress = null;
		VaadinAuthentication secondaryAuthn = (VaadinAuthentication) partialState.getSecondaryAuthenticator();
		
		AuthenticatorStepContext context = new AuthenticatorStepContext(endpointDescription.getRealm(), 
				partialState.getAuthenticationFlow(), null, FactorOrder.SECOND);
		Collection<VaadinAuthenticationUI> secondaryAuthnUIs = secondaryAuthn.createUIInstance(Context.LOGIN,
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
		VaadinAuthenticationUI secondaryUI = secondaryAuthnUIs.iterator().next();
		
		authNColumns.setVisible(false);
		
		SecondFactorAuthNPanel authNPanel = build2ndFactorAuthenticationOptionWidget(secondaryUI, partialState);
		AuthnOptionsColumn wrapping2ndFColumn = new AuthnOptionsColumn(null, 
				VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH);
		wrapping2ndFColumn.addOptions(Lists.newArrayList(
				new AuthnOptionsColumn.ComponentWithId("", authNPanel, 1, i -> Optional.empty())));
		secondFactorHolder.removeAllComponents();
		Label mfaInfo = new Label(msg.getMessage("AuthenticationUI.mfaRequired"));
		mfaInfo.addStyleName(Styles.error.toString());
		wrapping2ndFColumn.focusFirst();
		secondFactorHolder.addComponent(mfaInfo);
		secondFactorHolder.setComponentAlignment(mfaInfo, Alignment.TOP_CENTER);
		secondFactorHolder.addComponent(wrapping2ndFColumn);
		wrapping2ndFColumn.setWidthUndefined();
		secondFactorHolder.setComponentAlignment(wrapping2ndFColumn, Alignment.TOP_CENTER);
		secondFactorHolder.setVisible(true);
		rememberMeComponent.setVisible(
				getRememberMePolicy().equals(RememberMePolicy.allowFor2ndFactor));
	}
	
	private RememberMePolicy getRememberMePolicy()
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
		secondFactorHolder.removeAllComponents();
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
