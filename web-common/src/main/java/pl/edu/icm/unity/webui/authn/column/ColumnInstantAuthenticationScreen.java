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
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
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
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostFirstFactorAuthnDecision;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
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
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.remote.RemoteAuthnResponseProcessingFilter;
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
	private final MessageSource msg;
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
	private final WebAuthenticationProcessor authnProcessor;
	private final LocaleChoiceComponent localeChoice;
	private final List<AuthenticationFlow> flows;
	
	private AuthenticationOptionsHandler authnOptionsHandler;
	private FirstFactorAuthNPanel authNPanelInProgress;
	private CheckBox rememberMe;
	private AuthnOptionsColumns authNColumns;
	private VerticalLayout secondFactorHolder;
	private Component rememberMeComponent;
	private SandboxAuthnResultCallback sandboxCallback;
	private Component topHeader;
	private Component cancelComponent;
	private CredentialResetLauncher credentialResetLauncher;
	
	public ColumnInstantAuthenticationScreen(MessageSource msg, ImageAccessService imageAccessService, 
			VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			Supplier<Boolean> outdatedCredentialDialogLauncher,
			CredentialResetLauncher credentialResetLauncher,
			Runnable registrationLayoutLauncher, CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, boolean enableRegistration,
			Function<UnknownRemotePrincipalResult, UnknownUserDialog> unknownUserDialogProvider,
			WebAuthenticationProcessor authnProcessor,
			LocaleChoiceComponent localeChoice,
			List<AuthenticationFlow> flows)
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
		this.authnProcessor = authnProcessor;
		this.localeChoice = localeChoice;
		this.flows = flows;
		this.imageAccessService = imageAccessService;
		
		init();
	}

	@Override
	public void refresh(VaadinRequest request) 
	{
		log.debug("Refresh called on authN screen");
		refreshAuthenticationState();
		authNColumns.focusFirst();
	}

	@Override
	public void reset()
	{
		switchBackToPrimaryAuthentication();
	}
	
	protected void init()
	{
		log.debug("Authn screen init");
		this.authnOptionsHandler = new AuthenticationOptionsHandler(flows, endpointDescription.getName(), 
				endpointDescription.getRealm());
		
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
		refreshAuthenticationState();
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
		if (sandboxCallback != null)
			authnOption.authenticatorUI.setSandboxAuthnCallback(sandboxCallback);

		FirstFactorAuthNPanel authNPanel = new FirstFactorAuthNPanel(msg, execService, 
				cancelHandler, unknownUserDialogProvider, gridCompatible, 
				authnOption.authenticatorUI, optionId);
		FirstFactorAuthNResultCallback controller = new FirstFactorAuthNResultCallback(
				msg, authnProcessor, 
				endpointDescription.getRealm(), authnOption.flow, 
				this::isSetRememberMe, new PrimaryAuthenticationListenerImpl(
						optionId.toStringEncodedKey(), authNPanel), 
				optionId, endpointDescription.getEndpoint().getContextAddress(), 
				authNPanel);
		authnOption.authenticatorUI.setAuthenticationCallback(controller);
		authnOption.authenticatorUI.setCredentialResetLauncher(credentialResetLauncher);
		return authNPanel;
	}

	private SecondFactorAuthNPanel build2ndFactorAuthenticationOptionWidget(VaadinAuthenticationUI secondaryUI, 
			PartialAuthnState partialAuthnState)
	{
		AuthenticationOptionKey optionId = new AuthenticationOptionKey(
				partialAuthnState.getSecondaryAuthenticator().getAuthenticatorId(), 
				secondaryUI.getId());
		SecondaryAuthenticationListenerImpl listener = new SecondaryAuthenticationListenerImpl();
		SecondFactorAuthNPanel authNPanel = new SecondFactorAuthNPanel(msg, idsMan, execService, 
				secondaryUI, partialAuthnState, 
				optionId, listener);
		SecondFactorAuthNResultCallback controller = new SecondFactorAuthNResultCallback(msg, authnProcessor, 
				endpointDescription.getRealm(), listener, this::isSetRememberMe, 
				partialAuthnState, authNPanel);
		secondaryUI.setAuthenticationCallback(controller);
		secondaryUI.setCredentialResetLauncher(credentialResetLauncher);
		return authNPanel;
	}

	
	private boolean isSetRememberMe()
	{
		return rememberMe != null && rememberMe.getValue();
	}

	private void refreshAuthenticationState() 
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		PostFirstFactorAuthnDecision postFirstFactorDecision = (PostFirstFactorAuthnDecision) session
				.getAttribute(RemoteAuthnResponseProcessingFilter.DECISION_SESSION_ATTRIBUTE);
		if (postFirstFactorDecision != null)
		{
			log.debug("Remote authentication result found in session, triggering its processing");
			session.removeAttribute(RemoteAuthnResponseProcessingFilter.DECISION_SESSION_ATTRIBUTE);
			RedirectedAuthnFirstFactorResultProcessor remoteFirstFactorResultProcessor = 
					new RedirectedAuthnFirstFactorResultProcessor(msg, execService, 
							unknownUserDialogProvider);
			remoteFirstFactorResultProcessor.onCompletedAuthentication(postFirstFactorDecision);
		}
	}

	private void onAbortedAuthentication()
	{
		authNColumns.enableAll();
		enableSharedWidgets(true);
		authNPanelInProgress = null;
	}
	
	private void switchToSecondaryAuthentication(PartialAuthnState partialState)
	{
		enableSharedWidgets(true);
		authNPanelInProgress = null;
		VaadinAuthentication secondaryAuthn = (VaadinAuthentication) partialState.getSecondaryAuthenticator();
		
		AuthenticatorStepContext context = new AuthenticatorStepContext(endpointDescription.getRealm(), 
				partialState.getAuthenticationFlow(), 2);
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
	
	private class PrimaryAuthenticationListenerImpl implements FirstFactorAuthNResultCallback.AuthenticationListener
	{
		private final String optionId;
		private final FirstFactorAuthNPanel authNPanel;
		
		PrimaryAuthenticationListenerImpl(String selectedComponentId, FirstFactorAuthNPanel authNPanel)
		{
			this.optionId = selectedComponentId;
			this.authNPanel = authNPanel;
		}

		@Override
		public void authenticationStarted(boolean showProgress)
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
	
	private class SecondaryAuthenticationListenerImpl implements SecondFactorAuthNResultCallback.AuthenticationListener
	{
		@Override
		public void switchBackToFirstFactor()
		{
			switchBackToPrimaryAuthentication();
		}

		@Override
		public void authenticationStarted(boolean showProgress)
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
	
	//for sandbox extensions
	protected void setSandboxCallbackForAuthenticators(SandboxAuthnResultCallback callback) 
	{
		this.sandboxCallback = callback;
	}
}
