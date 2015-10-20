/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.Collection;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor.PartialAuthnState;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationResultCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormLauncher;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.VerticalLayout;

/**
 * The actual login component. Shows only the selected authenticator. 
 * 
 * @author K. Benedyczak
 */
public class SelectedAuthNPanel extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SelectedAuthNPanel.class);
	private static final long serialVersionUID = 1L;
	private UnityMessageSource msg;
	private WebAuthenticationProcessor authnProcessor;
	private IdentitiesManagement idsMan;
	private AuthenticationHandler currentAuthnResultCallback;
	private Button authenticateButton;
	private Button resetMfaButton;
	private CheckBox rememberMe;
	private InsecureRegistrationFormLauncher formLauncher;
	private ExecutorsService execService;
	private String clientIp;
	private AuthenticationRealm realm;
	private AuthenticationListener authNListener;
	
	private VerticalLayout authenticatorsContainer;
	private AuthenticationOption selectedAuthnOption;
	private VaadinAuthenticationUI primaryAuthnUI;
	private String authnId;
	private HorizontalLayout authnProgressHL;
	private String sandboxURL;
	private SandboxAuthnNotifier sandboxNotifier;
	private InputTranslationEngine inputTranslationEngine;
	
	
	public SelectedAuthNPanel(UnityMessageSource msg, WebAuthenticationProcessor authnProcessor,
			IdentitiesManagement idsMan,
			InsecureRegistrationFormLauncher formLauncher, ExecutorsService execService,
			final CancelHandler cancelHandler, AuthenticationRealm realm,
			String sandboxURL, SandboxAuthnNotifier sandboxNotifier,
			InputTranslationEngine inputTranslationEngine)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.idsMan = idsMan;
		this.formLauncher = formLauncher;
		this.execService = execService;
		this.realm = realm;
		this.sandboxURL = sandboxURL;
		this.sandboxNotifier = sandboxNotifier;
		this.inputTranslationEngine = inputTranslationEngine;

		VerticalLayout main = new VerticalLayout();
		main.addStyleName("u-selectedAuthn");
		main.setSpacing(true);
		main.setMargin(true);
		setSizeUndefined();
		authenticatorsContainer = new VerticalLayout();		
		authenticatorsContainer.setHeight(100, Unit.PERCENTAGE);
		
		authnProgressHL = new HorizontalLayout();
		authnProgressHL.setSpacing(true);
		
		ProgressBar progress = new ProgressBar();
		progress.setIndeterminate(true);
		progress.setCaption(msg.getMessage("AuthenticationUI.authnInProgress"));
		//cancellation of the ongoing (async) authentication
		Button cancelOngoingAuthnButton = new Button(msg.getMessage("cancel")); 
		cancelOngoingAuthnButton.addStyleName(Styles.vButtonSmall.toString());
		cancelOngoingAuthnButton.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				setNotAuthenticating();
				currentAuthnResultCallback.authenticationCancelled(true);
			}
		});
		authnProgressHL.addComponents(progress, cancelOngoingAuthnButton);
		showAuthnProgress(false);
		
		authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
		authenticateButton.setId("AuthenticationUI.authnenticateButton");
		authenticateButton.setVisible(false);
		
		rememberMe = new CheckBox(msg.getMessage("AuthenticationUI.rememberMe", 
				realm.getAllowForRememberMeDays()));

		authenticateButton.addClickListener(new LoginButtonListener());
		authenticateButton.setClickShortcut(KeyCode.ENTER);
		
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
		
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setSpacing(true);
		buttons.addComponents(authenticateButton, resetMfaButton);
		if (cancelHandler != null)
		{
			Button cancel = new Button(msg.getMessage("cancel")); //cancellation of the whole authN process
			cancel.addClickListener(new Button.ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					currentAuthnResultCallback.authenticationCancelled(true);
					cancelHandler.onCancel();
				}
			});
			buttons.addComponent(cancel);
		}
		
		main.addComponent(authnProgressHL);
		if (realm.getAllowForRememberMeDays() > 0)
			main.addComponent(rememberMe);
		main.addComponent(buttons);
		main.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
		setCompositionRoot(main);
	}

	/**
	 * @param primaryUI
	 */
	public void setAuthenticator(VaadinAuthenticationUI primaryAuthnUI, AuthenticationOption authnOption, 
			String id)
	{
		this.selectedAuthnOption = authnOption;
		this.primaryAuthnUI = primaryAuthnUI;
		this.authnId = id;
		primaryAuthnUI.clear();
		AuthenticationHandler primaryAuthnResultCallback = createPrimaryAuthnResultCallback(primaryAuthnUI);
		authenticatorsContainer.removeAllComponents();
		addRetrieval(primaryAuthnUI, primaryAuthnResultCallback);
		resetMfaButton.setVisible(false);
		authenticateButton.setVisible(true);
	}
	
	
	protected void showAuthnProgress(boolean inProgress)
	{
		authnProgressHL.setVisible(inProgress);
	}
	
	protected void handleError(String error)
	{
		setNotAuthenticating();
		NotificationPopup.showError(msg, msg.getMessage("AuthenticationUI.authnErrorTitle"), error);
	}
	
	/**
	 * Clears the UI so a new authentication can be started.
	 */
	protected void setNotAuthenticating()
	{
		authenticateButton.setEnabled(true);
		showAuthnProgress(false);
		if (authNListener != null)
			authNListener.authenticationStateChanged(false);
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
		showAuthnProgress(false);
		authenticateButton.setEnabled(true);
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
		AuthenticationHandler secondaryAuthnResultCallback = 
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
		else
			authenticateButton.focus();
	}
	
	private void addRetrieval(VaadinAuthenticationUI authnUI, AuthenticationHandler handler)
	{
		Component retrievalComponent = authnUI.getComponent();
		authenticatorsContainer.addComponent(retrievalComponent);
		updateFocus(retrievalComponent);
		authnUI.setAuthenticationResultCallback(handler);
		currentAuthnResultCallback = handler;
	}
	
	/**
	 * The method is separated as can be overridden in sandbox authn. 
	 * @return primary authentication result callback.
	 */
	protected AuthenticationHandler createPrimaryAuthnResultCallback(VaadinAuthenticationUI primaryAuthnUI)
	{
		return new PrimaryAuthenticationResultCallbackImpl(primaryAuthnUI);
	}

	/**
	 * The method is separated as can be overridden in sandbox authn. 
	 * @return secondary authentication result callback.
	 */
	protected AuthenticationHandler createSecondaryAuthnResultCallback(VaadinAuthenticationUI secondaryUI,
			PartialAuthnState partialState)
	{
		return new SecondaryAuthenticationResultCallbackImpl(secondaryUI, partialState);
	}
	
	private class LoginButtonListener implements ClickListener
	{
		private static final long serialVersionUID = 1L;
		
		public LoginButtonListener()
		{
		}

		@Override
		public void buttonClick(ClickEvent event)
		{
			if (!authenticateButton.isEnabled())
				return;
			
			clientIp = VaadinService.getCurrentRequest().getRemoteAddr();
			UnsuccessfulAuthenticationCounter counter = WebAuthenticationProcessor.getLoginCounter();
			if (counter.getRemainingBlockedTime(clientIp) > 0)
			{
				AccessBlockedDialog dialog = new AccessBlockedDialog(msg, execService);
				dialog.show();
				return;
			}
			
			if (authNListener != null)
				authNListener.authenticationStateChanged(true);

			AuthenticationUI.setLastIdpCookie(authnId);

			authenticateButton.setEnabled(false);

			//we copy the reference as current might be modified during the authentication even before 
			//the method return
			AuthenticationHandler savedHandler = currentAuthnResultCallback;
			savedHandler.triggerAuthentication();
			
			//if authenticator immediately returned the result, the authentication is already done now. 
			//Then showing cancel button&progress is unnecessary.
			if (!savedHandler.isAuthnDone())
				showAuthnProgress(true);
		}
	}


	/**
	 * Collects authN result from the first authenticator of the selected {@link AuthenticationOption} 
	 * and process it: manages state of the rest of the UI (cancel button, notifications, registration) 
	 * and if needed proceeds to 2nd authenticator. 
	 * 
	 * @author K. Benedyczak
	 */
	protected class PrimaryAuthenticationResultCallbackImpl implements AuthenticationHandler
	{
		protected VaadinAuthenticationUI authnUI;
		protected boolean authnDone = false;
		
		public PrimaryAuthenticationResultCallbackImpl(VaadinAuthenticationUI authnUI)
		{
			this.authnUI = authnUI;
		}

		@Override
		public void setAuthenticationResult(AuthenticationResult result)
		{
			log.trace("Received authentication result of the primary authenticator " + result);
			authnDone = true;
			try
			{
				PartialAuthnState partialState = authnProcessor.processPrimaryAuthnResult(
						result, clientIp, realm, 
						selectedAuthnOption, rememberMe.getValue());
				if (partialState == null)
				{
					setNotAuthenticating();
					if (authNListener != null)
						authNListener.clearUI();
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
				handleError(msg.getMessage(e.getMessage()));
			}
		}
		
		@Override
		public void cancelAuthentication()
		{
			authenticationCancelled(false);
		}
		
		@Override
		public void authenticationCancelled(boolean signalAuthenticators)
		{
			setNotAuthenticating();
			authnDone = true;
			if (signalAuthenticators)
			{
				authnUI.cancelAuthentication();
				authnUI.clear();
			}
		}
		
		@Override
		public boolean isAuthnDone()
		{
			return authnDone;
		}

		@Override
		public void triggerAuthentication()
		{
			authnDone = false;
			authnUI.triggerAuthentication();
		}
		
		@Override
		public void refresh(VaadinRequest request)
		{
			authnUI.refresh(request);
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
		public void setAuthenticationResult(AuthenticationResult result)
		{
			log.trace("Received authentication result of the 2nd authenticator" + result);
			authnDone = true;
			try
			{
				authnProcessor.processSecondaryAuthnResult(partialState, result, clientIp, realm, 
						selectedAuthnOption, rememberMe.getValue());
				setNotAuthenticating();
				if (authNListener != null)
					authNListener.clearUI();
			} catch (AuthenticationException e)
			{
				log.trace("Secondary authentication failed ", e);
				handleError(msg.getMessage(e.getMessage()));
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
			handleError(msg.getMessage("AuthenticationUI.unknownRemoteUser"));
		}
	}

	protected void showUnknownUserDialog(UnknownRemoteUserException ee)
	{
		setNotAuthenticating();
		UnknownUserDialog dialog = new UnknownUserDialog(msg, ee.getResult(), 
				formLauncher, sandboxNotifier, inputTranslationEngine, sandboxURL);
		dialog.show();
	}	
	
	public void refresh(VaadinRequest request)
	{
		if (currentAuthnResultCallback != null)
			currentAuthnResultCallback.refresh(request);
	}
	
	/**
	 * listener to be registered to the authentication button.
	 * @param listener
	 */
	public void setAuthenticationListener(AuthenticationListener listener)
	{
		this.authNListener = listener;
	}
	
	public interface AuthenticationListener
	{
		void authenticationStateChanged(boolean started);
		void clearUI();
	}
	
	/**
	 * Extends {@link AuthenticationResultCallback} with internal operations which are used to pass actions
	 * to the selected {@link VaadinAuthenticationUI}
	 * @author K. Benedyczak
	 */
	public interface AuthenticationHandler extends AuthenticationResultCallback
	{
		void triggerAuthentication();
		
		void authenticationCancelled(boolean signalAuthenticators);
		
		boolean isAuthnDone();
		
		void refresh(VaadinRequest request);
	}

}
