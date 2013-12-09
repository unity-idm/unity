/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.AuthenticationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.webui.ActivationListener;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.UsernameProvider;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.registration.RegistrationRequestEditorDialog;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

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
	private Button authenticateButton;
	private UsernameComponent usernameComponent;
	private InsecureRegistrationFormLauncher formLauncher;
	private ExecutorsService execService;
	
	public AuthenticatorSetComponent(Map<String, VaadinAuthentication> authenticators,
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
			VaadinAuthentication vaadinAuth = authenticators.get(authenticator); 
			if (vaadinAuth.needsCommonUsernameComponent())
				needCommonUsername = true;
			authenticatorsContainer.addComponent(vaadinAuth.getComponent());
			authenticatorsContainer.addComponent(new Label("<hr>", ContentMode.HTML));
		}
		
		authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
		usernameComponent = null;
		if (needCommonUsername)
		{
			usernameComponent = new UsernameComponent();
			addComponent(usernameComponent);
			for (String authenticator: set.getAuthenticators())
			{
				VaadinAuthentication vaadinAuth = authenticators.get(authenticator); 
				if (vaadinAuth.needsCommonUsernameComponent())
					vaadinAuth.setUsernameCallback(usernameComponent);
			}
		}

		authenticateButton.addClickListener(new LoginButtonListener(authenticators, set, usernameComponent));
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
		
		addComponent(buttons);
		setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
	}
		
	private class LoginButtonListener implements ClickListener
	{
		private static final long serialVersionUID = 1L;
		private Map<String, VaadinAuthentication> authenticators;
		private AuthenticatorSet set;
		private UsernameComponent usernameComp;
		
		public LoginButtonListener(Map<String, VaadinAuthentication> authenticators,
				AuthenticatorSet set, UsernameComponent usernameComp)
		{
			this.authenticators = authenticators;
			this.set = set;
			this.usernameComp = usernameComp;
		}

		@Override
		public void buttonClick(ClickEvent event)
		{
			String ip = VaadinService.getCurrentRequest().getRemoteAddr();
			UnsuccessfulAuthenticationCounter counter = AuthenticationProcessor.getLoginCounter();
			if (counter.getRemainingBlockedTime(ip) > 0)
			{
				AccessBlockedDialog dialog = new AccessBlockedDialog(msg, execService);
				dialog.show();
				return;
			}
			
			List<AuthenticationResult> results = new ArrayList<AuthenticationResult>();
			for (String authenticator: set.getAuthenticators())
			{
				VaadinAuthentication vaadinAuth = authenticators.get(authenticator);
				results.add(vaadinAuth.getAuthenticationResult());
			}
			
			try
			{
				authnProcessor.processResults(results);
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
			ErrorPopup.showError(msg, error, "");
		}
	}
	
	
	private class UsernameComponent extends HorizontalLayout implements UsernameProvider
	{
		private static final long serialVersionUID = 1L;
		private TextField username;
		
		public UsernameComponent()
		{
			username = new TextField(msg.getMessage("AuthenticationUI.username"));
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
