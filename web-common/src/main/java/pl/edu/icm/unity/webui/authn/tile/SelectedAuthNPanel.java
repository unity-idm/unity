/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.tile;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.AccessBlockedDialog;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationStyle;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * The actual login component. Shows only the selected authenticator. 
 * 
 * @author K. Benedyczak
 */
public class SelectedAuthNPanel extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SelectedAuthNPanel.class);
	private UnityMessageSource msg;
	private WebAuthenticationProcessor authnProcessor;
	private EntityManagement idsMan;
	private AuthenticationUIController currentAuthnResultCallback;
	private Button resetMfaButton;
	private ExecutorsService execService;
	private String clientIp;
	private AuthenticationRealm realm;
	private AuthenticationListener authNListener;
	private final Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider; 
	
	private VerticalLayout authenticatorsContainer;
	private AuthenticationFlow selectedAuthnOption;
	private VaadinAuthenticationUI primaryAuthnUI;
	private String authnId;
	private String endpointPath;
	private Supplier<Boolean> rememberMeProvider;
	
	
	public SelectedAuthNPanel(UnityMessageSource msg, WebAuthenticationProcessor authnProcessor,
			EntityManagement idsMan, ExecutorsService execService,
			CancelHandler cancelHandler, AuthenticationRealm realm,
			String endpointPath, 
			Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider,
			Supplier<Boolean> rememberMeProvider)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.idsMan = idsMan;
		this.execService = execService;
		this.realm = realm;
		this.endpointPath = endpointPath;
		this.unknownUserDialogProvider = unknownUserDialogProvider;
		this.rememberMeProvider = rememberMeProvider;

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addStyleName("u-selectedAuthn");
		
		main.setWidth(100, Unit.PERCENTAGE);
		authenticatorsContainer = new VerticalLayout();		
		authenticatorsContainer.setHeight(100, Unit.PERCENTAGE);
		authenticatorsContainer.setWidth(100, Unit.PERCENTAGE);
		authenticatorsContainer.setSpacing(false);
		authenticatorsContainer.setMargin(false);

		resetMfaButton = new Button(msg.getMessage("AuthenticationUI.resetMfaButton"));
		resetMfaButton.setDescription(msg.getMessage("AuthenticationUI.resetMfaButtonDesc"));
		resetMfaButton.setVisible(false);
		resetMfaButton.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				setNotAuthenticating();
				switchToPrimaryAuthentication();
			}
		});
		
		main.addComponent(authenticatorsContainer);
		main.setComponentAlignment(authenticatorsContainer, Alignment.MIDDLE_CENTER);
		
		//TODO MFA
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setMargin(false);
		buttons.addComponents(resetMfaButton);
		buttons.setVisible(false);
		
		main.addComponent(buttons);
		main.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
		setCompositionRoot(main);
	}

	/**
	 * @param primaryUI
	 */
	public void setAuthenticator(VaadinAuthenticationUI primaryAuthnUI, AuthenticationFlow authnOption, String id)
	{
		this.selectedAuthnOption = authnOption;
		this.primaryAuthnUI = primaryAuthnUI;
		this.authnId = id;
		primaryAuthnUI.clear();
		AuthenticationUIController primaryAuthnResultCallback = createPrimaryAuthnResultCallback(primaryAuthnUI);
		authenticatorsContainer.removeAllComponents();
		addRetrieval(primaryAuthnUI, primaryAuthnResultCallback);
		resetMfaButton.setVisible(false);
	}
	
	public void removeEnterKeyBinding()
	{
		//authenticateButton.removeClickShortcut();
		//TODO
	}
	
	public void restoreEnterKeyBinding()
	{
		//authenticateButton.setClickShortcut(KeyCode.ENTER);
		//TODO
	}
	
	protected void handleError(String genericError, String authenticatorError)
	{
		setNotAuthenticating();
		if (authenticatorsContainer.getComponentCount() > 0)
			updateFocus(authenticatorsContainer.getComponent(0));
		String errorToShow = authenticatorError == null ? genericError : authenticatorError;
		NotificationPopup.showError(msg, msg.getMessage("AuthenticationUI.authnErrorTitle"), errorToShow);
		showWaitScreenIfNeeded();
	}
	
	public String getAuthenticationOptionId()
	{
		return authnId;
	}
	
	/**
	 * Clears the UI so a new authentication can be started.
	 */
	protected void setNotAuthenticating()
	{
		if (authNListener != null)
			authNListener.authenticationStopped();
	}
	
	/**
	 * Resets the authentication UI to the initial state
	 */
	private void switchToPrimaryAuthentication()
	{
		primaryAuthnUI.getComponent().setEnabled(true);
		setAuthenticator(primaryAuthnUI, selectedAuthnOption, authnId);
	}

	
	private void switchToSecondaryAuthentication(PartialAuthnState partialState)
	{
		primaryAuthnUI.getComponent().setEnabled(false);
		resetMfaButton.setVisible(true);
		
		VaadinAuthentication secondaryAuthn = (VaadinAuthentication) 
				partialState.getSecondaryAuthenticator();
		Collection<VaadinAuthenticationUI> secondaryAuthnUIs = secondaryAuthn.createUIInstance();
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
		AuthenticationUIController secondaryAuthnResultCallback = 
				createSecondaryAuthnResultCallback(secondaryUI, partialState);
		Label mfaInfo = new Label(msg.getMessage("AuthenticationUI.mfaRequired"));
		mfaInfo.addStyleName(Styles.error.toString());
		authenticatorsContainer.addComponents(HtmlTag.br(), mfaInfo);
		addRetrieval(secondaryUI, secondaryAuthnResultCallback);
		try
		{
			secondaryUI.presetEntity(resolveEntity(partialState.getPrimaryResult()));
		} catch (EngineException e)
		{
			log.error("Can't resolve the first authenticated entity", e);
		}
	}
	
	private Entity resolveEntity(AuthenticationResult unresolved) throws EngineException
	{
		AuthenticatedEntity ae = unresolved.getAuthenticatedEntity();
		return idsMan.getEntity(new EntityParam(ae.getEntityId()));
	}
	
	private void updateFocus(Component retrievalComponent)
	{
		if (retrievalComponent instanceof Focusable)
			((Focusable)retrievalComponent).focus();
	}
	
	private void addRetrieval(VaadinAuthenticationUI authnUI, AuthenticationUIController handler)
	{
		Component retrievalComponent = authnUI.getComponent();
		authenticatorsContainer.addComponent(retrievalComponent);
		updateFocus(retrievalComponent);
		authnUI.setAuthenticationCallback(handler);
		currentAuthnResultCallback = handler;
	}
	
	/**
	 * The method is separated as can be overridden in sandbox authn. 
	 * @return primary authentication result callback.
	 */
	protected AuthenticationUIController createPrimaryAuthnResultCallback(VaadinAuthenticationUI primaryAuthnUI)
	{
		return new PrimaryAuthenticationResultCallbackImpl(primaryAuthnUI);
	}

	/**
	 * The method is separated as can be overridden in sandbox authn. 
	 * @return secondary authentication result callback.
	 */
	protected AuthenticationUIController createSecondaryAuthnResultCallback(VaadinAuthenticationUI secondaryUI,
			PartialAuthnState partialState)
	{
		return new SecondaryAuthenticationResultCallbackImpl(secondaryUI, partialState);
	}
	
	private void onAuthenticationStart(AuthenticationStyle style)
	{
		clientIp = VaadinService.getCurrentRequest().getRemoteAddr();
		if (authNListener != null && style != AuthenticationStyle.IMMEDIATE)
			authNListener.authenticationStarted(style == AuthenticationStyle.WITH_EXTERNAL_CANCEL);
		setLastIdpCookie(authnId);
	}

	private void showWaitScreenIfNeeded()
	{
		UnsuccessfulAuthenticationCounter counter = WebAuthenticationProcessor.getLoginCounter();
		if (counter.getRemainingBlockedTime(clientIp) > 0)
		{
			AccessBlockedDialog dialog = new AccessBlockedDialog(msg, execService);
			dialog.show();
			return;
		}
	}
	
	private void setLastIdpCookie(String idpKey)
	{
		if (endpointPath == null)
			return;
		VaadinResponse resp = VaadinService.getCurrentResponse();
		resp.addCookie(PreferredAuthenticationHelper.createLastIdpCookie(endpointPath, idpKey));
	}
	
	/**
	 * Collects authN result from the first authenticator of the selected {@link AuthenticationFlow} 
	 * and process it: manages state of the rest of the UI (cancel button, notifications, registration) 
	 * and if needed proceeds to 2nd authenticator. 
	 * 
	 * @author K. Benedyczak
	 */
	protected class PrimaryAuthenticationResultCallbackImpl implements AuthenticationUIController
	{
		protected VaadinAuthenticationUI authnUI;
		protected boolean authnDone = false;
		
		public PrimaryAuthenticationResultCallbackImpl(VaadinAuthenticationUI authnUI)
		{
			this.authnUI = authnUI;
		}

		@Override
		public void onCompletedAuthentication(AuthenticationResult result)
		{
			processAuthn(result, null);
		}
		

		@Override
		public void onFailedAuthentication(AuthenticationResult result, String error,
				Optional<String> errorDetail)
		{
			processAuthn(result, error);
		}
		
		protected void processAuthn(AuthenticationResult result, String error)
		{
			log.trace("Received authentication result of the primary authenticator " + result);
			authnDone = true;
			try
			{
				PartialAuthnState partialState = authnProcessor.processPrimaryAuthnResult(
						result, clientIp, realm, 
						selectedAuthnOption, rememberMeProvider.get());
				if (partialState == null)
				{
					setNotAuthenticating();
				} else
				{
					switchToSecondaryAuthentication(partialState);
				}
			} catch (UnknownRemoteUserException e)
			{
				handleUnknownUser(e);
			} catch (AuthenticationException e)
			{
				log.trace("Authentication failed ", e);
				handleError(msg.getMessage(e.getMessage()), error);
			}
		}

		@Override
		public void onStartedAuthentication(AuthenticationStyle style)
		{
			onAuthenticationStart(style);
		}

		@Override
		public void onCancelledAuthentication()
		{
			setNotAuthenticating();
		}
		
		@Override
		public void refresh(VaadinRequest request)
		{
			authnUI.refresh(request);
		}
		
		@Override
		public void cancel()
		{
			authnUI.clear();
		}
	}

	/**
	 * Collects authN results from the 2nd authenticator. Afterwards, the final authentication result 
	 * processing is launched.
	 * 
	 * @author K. Benedyczak
	 */
	protected class SecondaryAuthenticationResultCallbackImpl extends PrimaryAuthenticationResultCallbackImpl
	{
		private PartialAuthnState partialState;
		
		public SecondaryAuthenticationResultCallbackImpl(VaadinAuthenticationUI authnUI,
				PartialAuthnState partialState)
		{
			super(authnUI);
			this.partialState = partialState;
		}

		@Override
		public void onCompletedAuthentication(AuthenticationResult result)
		{
			log.trace("Received authentication result of the 2nd authenticator" + result);
			authnDone = true;
			try
			{
				authnProcessor.processSecondaryAuthnResult(partialState, result, clientIp, realm, 
						selectedAuthnOption, rememberMeProvider.get());
				setNotAuthenticating();
			} catch (AuthenticationException e)
			{
				log.trace("Secondary authentication failed ", e);
				handleError(msg.getMessage(e.getMessage()), null);
				switchToPrimaryAuthentication();
			}
		}
	}

	protected void handleUnknownUser(UnknownRemoteUserException e)
	{
		if (e.getFormForUser() != null || e.getResult().isEnableAssociation())
		{
			log.trace("Authentication successful, user unknown, "
					+ "showing unknown user dialog");
			showUnknownUserDialog(e);
		} else
		{
			log.trace("Authentication successful, user unknown, "
					+ "no registration form");
			handleError(msg.getMessage("AuthenticationUI.unknownRemoteUser"), null);
		}
	}

	protected void showUnknownUserDialog(UnknownRemoteUserException ee)
	{
		setNotAuthenticating();
		UnknownUserDialog dialog = unknownUserDialogProvider.apply(ee.getResult()); 
		dialog.show();
	}	
	
	public void refresh(VaadinRequest request)
	{
		if (currentAuthnResultCallback != null)
			currentAuthnResultCallback.refresh(request);
	}
	
	public void cancel()
	{
		if (currentAuthnResultCallback != null)
			currentAuthnResultCallback.cancel();
	}
	
	/**
	 * listener to be registered to the authentication button.
	 * @param listener
	 */
	public void setAuthenticationListener(AuthenticationListener listener)
	{
		this.authNListener = listener;
	}
	
	/**
	 * Used by upstream code holding this component to be informed about changes in this component. 
	 */
	public interface AuthenticationListener
	{
		void authenticationStarted(boolean showProgress);
		void authenticationStopped();
	}
	
	/**
	 * Internal operations which are used to pass actions to the selected {@link VaadinAuthenticationUI}
	 */
	public interface AuthenticationUIController extends AuthenticationCallback
	{
		void cancel();
		void refresh(VaadinRequest request);
	}
}
