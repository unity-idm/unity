/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.webui.ActivationListener;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationResultCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.UsernameProvider;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.registration.RegistrationRequestEditorDialog;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.Reindeer;

/**
 * Displays authenticators set.
 * <p>
 * The set may be decorated with a link to start the registration procedure.
 * @author K. Benedyczak
 */
public class AuthenticatorSetComponent extends VerticalLayout implements ActivationListener
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticatorSetComponent.class);
	private static final long serialVersionUID = 1L;
	private UnityMessageSource msg;
	private AuthenticationProcessor authnProcessor;
	private AuthenticationResultCallbackImpl authnResultCallback;
	private Button authenticateButton;
	private Button cancelButton;
	private ProgressIndicator progress;
	private UsernameComponent usernameComponent;
	private InsecureRegistrationFormLauncher formLauncher;
	private ExecutorsService execService;
	private String clientIp;
	
	public AuthenticatorSetComponent(Map<String, VaadinAuthenticationUI> authenticators,
			AuthenticatorSet set, UnityMessageSource msg, AuthenticationProcessor authnProcessor,
			InsecureRegistrationFormLauncher formLauncher, ExecutorsService execService,
			final CancelHandler cancelHandler)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.formLauncher = formLauncher;
		this.execService = execService;
		boolean needCommonUsername = false;
		setSpacing(true);
		setMargin(true);
		setSizeUndefined();
		VerticalLayout authenticatorsContainer = new VerticalLayout();		
		authenticatorsContainer.setSpacing(true);
		authenticatorsContainer.addComponent(new Label("<hr>", ContentMode.HTML));
		for (String authenticator: set.getAuthenticators())
		{
			VaadinAuthenticationUI vaadinAuth = authenticators.get(authenticator); 
			if (vaadinAuth.needsCommonUsernameComponent())
				needCommonUsername = true;
			vaadinAuth.setAuthenticationResultCallback(authnResultCallback);
			authenticatorsContainer.addComponent(vaadinAuth.getComponent());
			authenticatorsContainer.addComponent(new Label("<hr>", ContentMode.HTML));
		}
		
		
		HorizontalLayout authnProgressHL = new HorizontalLayout();
		authnProgressHL.setSpacing(true);
		
		progress = new ProgressIndicator();
		progress.setIndeterminate(true);
		progress.setCaption(msg.getMessage("AuthenticationUI.authnInProgress"));
		cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addStyleName(Reindeer.BUTTON_SMALL);
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
		
		authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
		authenticateButton.setId("AuthenticationUI.authnenticateButton");
		usernameComponent = null;
		if (needCommonUsername)
		{
			usernameComponent = new UsernameComponent();
			addComponent(usernameComponent);
			for (String authenticator: set.getAuthenticators())
			{
				VaadinAuthenticationUI vaadinAuth = authenticators.get(authenticator); 
				if (vaadinAuth.needsCommonUsernameComponent())
					vaadinAuth.setUsernameCallback(usernameComponent);
			}
		}

		authnResultCallback = new AuthenticationResultCallbackImpl(authenticators, usernameComponent);
		for (String authenticator: set.getAuthenticators())
		{
			VaadinAuthenticationUI vaadinAuth = authenticators.get(authenticator); 
			vaadinAuth.setAuthenticationResultCallback(authnResultCallback);
		}
		
		authenticateButton.addClickListener(new LoginButtonListener(authenticators, set));
		addComponent(authenticatorsContainer);
		
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
					cancelHandler.onCancel();
				}
			});
			buttons.addComponent(cancel);
		}
		
		addComponent(authnProgressHL);
		addComponent(buttons);
		setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
	}

	private void showAuthnProgress(boolean inProgress)
	{
		progress.setVisible(inProgress);
		cancelButton.setVisible(inProgress);
	}
	
	private class LoginButtonListener implements ClickListener
	{
		private static final long serialVersionUID = 1L;
		private Map<String, VaadinAuthenticationUI> authenticators;
		private AuthenticatorSet set;
		
		public LoginButtonListener(Map<String, VaadinAuthenticationUI> authenticators,
				AuthenticatorSet set)
		{
			this.authenticators = authenticators;
			this.set = set;
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
			
			authenticateButton.setEnabled(false);
			authnResultCallback.resetAuthnDone();
			
			for (String authenticator: set.getAuthenticators())
			{
				VaadinAuthenticationUI vaadinAuth = authenticators.get(authenticator);
				vaadinAuth.triggerAuthentication();
			}
			
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
	private class AuthenticationResultCallbackImpl implements AuthenticationResultCallback
	{
		private List<AuthenticationResult> results = new ArrayList<AuthenticationResult>();
		private Map<String, VaadinAuthenticationUI> authenticators;
		private final int numberOfAuthenticators;
		private UsernameComponent usernameComp;
		private boolean authnDone = false;
		
		public AuthenticationResultCallbackImpl(Map<String, VaadinAuthenticationUI> authenticators,
				UsernameComponent usernameComp)
		{
			this.authenticators = authenticators;
			numberOfAuthenticators = authenticators.size();
			this.usernameComp = usernameComp;
		}

		@Override
		public synchronized void setAuthenticationResult(AuthenticationResult result)
		{
			results.add(result);
			if (results.size() == numberOfAuthenticators)
				authnDone();
		}

		@Override
		public synchronized void cancelAuthentication()
		{
			authenticationCancelled(false);
		}
		
		public synchronized void authenticationCancelled(boolean signalAuthenticators)
		{
			this.results.clear();
			authenticateButton.setEnabled(true);
			showAuthnProgress(false);
			authnDone = true;
			if (signalAuthenticators)
			{
				for (VaadinAuthenticationUI vaadinAuth: authenticators.values())
					vaadinAuth.cancelAuthentication();
			}
		}
		
		public synchronized void resetAuthnDone()
		{
			authnDone = false;
		}
		
		public synchronized boolean isAuthnDone()
		{
			return authnDone;
		}
		
		private void authnDone()
		{
			VaadinSession session = VaadinSession.getCurrent();
			session.lock();
			try
			{
				List<AuthenticationResult> results = new ArrayList<>(this.results);
				this.results.clear();
				authenticateButton.setEnabled(true);
				authnDone = true;

				try
				{
					authnProcessor.processResults(results, clientIp);
				} catch (UnknownRemoteUserException e)
				{
					if (e.getFormForUser() != null)
					{
						showRegistration(e);
						return;
					} else
					{
						handleError(msg.getMessage("AuthenticationUI.unknownRemoteUser"));
					}
				} catch (AuthenticationException e)
				{
					handleError(msg.getMessage(e.getMessage()));
				}
				showAuthnProgress(false);
			} finally
			{
				session.unlock();
			}
		}
		
		private void showRegistration(UnknownRemoteUserException ee)
		{
			RegistrationRequestEditorDialog dialog;
			try
			{
				dialog = formLauncher.getDialog(ee.getFormForUser(), ee.getRemoteContext());
				dialog.show();
			} catch (EngineException e)
			{
				log.error("Can't show a registration form for the remotely authenticated user as configured. " +
						"Probably the form name is wrong.", e);
				handleError(msg.getMessage("AuthenticationUI.problemWithRegistration"));
			}
		}
		
		private void handleError(String error)
		{
			if (usernameComp != null)
				usernameComp.setError(error);
			ErrorPopup.showError(msg, msg.getMessage("AuthenticationUI.authnErrorTitle"), error);
		}
	}
	
	
	
	private class UsernameComponent extends HorizontalLayout implements UsernameProvider
	{
		private static final long serialVersionUID = 1L;
		private TextField username;
		
		public UsernameComponent()
		{
			username = new TextField(msg.getMessage("AuthenticationUI.username"));
			username.setId("AuthenticationUI.username");
			addComponent(username);
		}

		@Override
		public String getUsername()
		{
			return username.getValue();
		}
		
		public void setError(String error)
		{
			username.setComponentError(new UserError(error));
		}
		
		public void setFocus()
		{
			username.focus();
		}
	}

	@Override
	public void stateChanged(boolean enabled)
	{
		if (enabled)
		{
			authenticateButton.setClickShortcut(KeyCode.ENTER);
			if (usernameComponent != null)
				usernameComponent.setFocus();
		} else
		{
			authenticateButton.removeClickShortcut();
		}
	}
}
