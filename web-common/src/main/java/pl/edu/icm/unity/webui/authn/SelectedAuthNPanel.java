/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationResultCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.registration.RegistrationRequestEditorDialog;

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
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.VerticalLayout;

/**
 * The actual login component. Shows only the selected authenticator. 
 * 
 * TODO: for 2factor authN the second authentication option should be presented after the first one is triggered.
 * 
 * @author K. Benedyczak
 */
public class SelectedAuthNPanel extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SelectedAuthNPanel.class);
	private static final long serialVersionUID = 1L;
	private UnityMessageSource msg;
	private AuthenticationProcessor authnProcessor;
	private AuthenticationResultProcessor authnResultCallback;
	private Button authenticateButton;
	private Button cancelButton;
	private ProgressBar progress;
	private CheckBox rememberMe;
	private InsecureRegistrationFormLauncher formLauncher;
	private ExecutorsService execService;
	private String clientIp;
	private AuthenticationRealm realm;
	
	private VerticalLayout authenticatorsContainer;
	private VaadinAuthenticationUI primaryAuthnUI;
	private String authnId;
	
	
	public SelectedAuthNPanel(UnityMessageSource msg, AuthenticationProcessor authnProcessor,
			InsecureRegistrationFormLauncher formLauncher, ExecutorsService execService,
			final CancelHandler cancelHandler, AuthenticationRealm realm)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.formLauncher = formLauncher;
		this.execService = execService;
		this.realm = realm;

		
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(true);
		setSizeUndefined();
		authenticatorsContainer = new VerticalLayout();		
		authenticatorsContainer.setHeight(100, Unit.PERCENTAGE);
		
		HorizontalLayout authnProgressHL = new HorizontalLayout();
		authnProgressHL.setSpacing(true);
		
		progress = new ProgressBar();
		progress.setIndeterminate(true);
		progress.setCaption(msg.getMessage("AuthenticationUI.authnInProgress"));
		cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addStyleName(Styles.vButtonSmall.toString());
		cancelButton.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				authnResultCallback.authenticationCancelled(true);
			}
		});
		authnProgressHL.addComponents(progress, cancelButton);
		showAuthnProgress(false);
		
		authnResultCallback = createAuthnResultCallback();
		
		authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
		authenticateButton.setId("AuthenticationUI.authnenticateButton");
		
		rememberMe = new CheckBox(msg.getMessage("AuthenticationUI.rememberMe", 
				realm.getAllowForRememberMeDays()));

		authenticateButton.addClickListener(new LoginButtonListener());
		authenticateButton.setClickShortcut(KeyCode.ENTER);
		main.addComponent(authenticatorsContainer);
		
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setSpacing(true);
		buttons.addComponent(authenticateButton);
		if (cancelHandler != null)
		{
			Button cancel = new Button(msg.getMessage("cancel"));
			cancel.addClickListener(new Button.ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					clearAuthenticators();
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
	 * TODO - should also pass and handle the whole authenticator set with all the settings.
	 * @param primaryUI
	 */
	public void setAuthenticator(VaadinAuthenticationUI primaryUI, String id)
	{
		this.primaryAuthnUI = primaryUI;
		this.authnId = id;
		primaryAuthnUI.setAuthenticationResultCallback(authnResultCallback);
		authenticatorsContainer.removeAllComponents();
		Component retrievalComponent = primaryUI.getComponent();
		authenticatorsContainer.addComponent(retrievalComponent);
		if (retrievalComponent instanceof Focusable)
			((Focusable)retrievalComponent).focus();
		else
			authenticateButton.focus();
	}
	
	
	protected void showAuthnProgress(boolean inProgress)
	{
		log.trace("Authn progress visible: " + inProgress);
		progress.setVisible(inProgress);
		cancelButton.setVisible(inProgress);
	}
	
	protected void clearAuthenticators()
	{
		primaryAuthnUI.clear();
	}
	
	protected AuthenticationResultProcessor createAuthnResultCallback()
	{
		return new AuthenticationResultCallbackImpl();
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
			UnsuccessfulAuthenticationCounter counter = AuthenticationProcessor.getLoginCounter();
			if (counter.getRemainingBlockedTime(clientIp) > 0)
			{
				AccessBlockedDialog dialog = new AccessBlockedDialog(msg, execService);
				dialog.show();
				return;
			}
			
			AuthenticationUI.setLastIdpCookie(authnId);

			
			authenticateButton.setEnabled(false);
			authnResultCallback.resetAuthnDone();
			
			primaryAuthnUI.triggerAuthentication();
			
			//if all authenticators immediately returned the result authentication is already done now, 
			// after all triggers. Then showing cancel button&progress is unnecessary.
			if (!authnResultCallback.isAuthnDone())
				showAuthnProgress(true);
		}
	}

	/**
	 * Collects authN results from all authenticators of this set. When all required results are
	 * there, the final authentication result processing is launched. Also cancellation handling is 
	 * implemented here.
	 * 
	 * @author K. Benedyczak
	 */
	protected class AuthenticationResultCallbackImpl implements AuthenticationResultProcessor
	{
		private List<AuthenticationResult> results = new ArrayList<AuthenticationResult>();
		private final int numberOfAuthenticators;
		private boolean authnDone = false;
		
		public AuthenticationResultCallbackImpl()
		{
			numberOfAuthenticators = 1;
		}

		@Override
		public void setAuthenticationResult(AuthenticationResult result)
		{
			log.trace("Received authentication result nr " + (results.size() + 1));
			results.add(result);
			if (results.size() == numberOfAuthenticators)
				authnDone();
		}

		@Override
		public void cancelAuthentication()
		{
			authenticationCancelled(false);
		}
		
		public void authenticationCancelled(boolean signalAuthenticators)
		{
			results.clear();
			authenticateButton.setEnabled(true);
			showAuthnProgress(false);
			authnDone = true;
			if (signalAuthenticators)
			{
				primaryAuthnUI.cancelAuthentication();
			}
		}
		
		public void resetAuthnDone()
		{
			authnDone = false;
		}
		
		public boolean isAuthnDone()
		{
			return authnDone;
		}
		
		protected void cleanAuthentication()
		{
			this.results.clear();
			authenticateButton.setEnabled(true);
			authnDone = true;
			clearAuthenticators();
		}
		
		protected void authnDone()
		{
			log.trace("Authentication completed, starting processing.");
			List<AuthenticationResult> results = new ArrayList<>(this.results);
			cleanAuthentication();
			try
			{
				authnProcessor.processResults(results, clientIp, realm, rememberMe.getValue());
			} catch (UnknownRemoteUserException e)
			{
				if (e.getFormForUser() != null)
				{
					log.trace("Authentication successful, user unknown, "
							+ "showing registration form");
					showRegistration(e);
				} else
				{
					log.trace("Authentication successful, user unknown, "
							+ "no registration form");
					handleError(msg.getMessage("AuthenticationUI.unknownRemoteUser"));
				}
			} catch (AuthenticationException e)
			{
				log.trace("Authentication failed ", e);
				handleError(msg.getMessage(e.getMessage()));
			}
			showAuthnProgress(false);
		}
		
		protected void showRegistration(UnknownRemoteUserException ee)
		{
			RegistrationRequestEditorDialog dialog;
			try
			{
				dialog = formLauncher.getDialog(ee.getFormForUser(), ee.getRemoteContext());
				dialog.show();
			} catch (AuthenticationException e)
			{
				log.debug("Can't show a registration form for the remotely authenticated user - "
						+ "user does not meet form requirements.", e);
				handleError(msg.getMessage("AuthenticationUI.infufficientRegistrationInput"));
			} catch (EngineException e)
			{
				log.error("Can't show a registration form for the remotely authenticated user as configured. " +
						"Probably the form name is wrong.", e);
				handleError(msg.getMessage("AuthenticationUI.problemWithRegistration"));
			}
		}
		
		protected void handleError(String error)
		{
			ErrorPopup.showError(msg, msg.getMessage("AuthenticationUI.authnErrorTitle"), error);
		}
	}
	
	public void refresh(VaadinRequest request)
	{
		if (primaryAuthnUI != null)
			primaryAuthnUI.refresh(request);
	}
	
	/**
	 * Extends {@link AuthenticationResultCallback} with internal operations needed by the 
	 * {@link AuthenticatorSetComponent}: handling of authentication finish.
	 * @author K. Benedyczak
	 */
	public interface AuthenticationResultProcessor extends AuthenticationResultCallback
	{
		void authenticationCancelled(boolean signalAuthenticators);
		
		void resetAuthnDone();
		
		boolean isAuthnDone();
	}

}
