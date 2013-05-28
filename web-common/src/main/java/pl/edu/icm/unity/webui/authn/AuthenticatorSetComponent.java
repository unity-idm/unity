/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.exceptions.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.webui.ActivationListener;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.UsernameProvider;
import pl.edu.icm.unity.webui.common.ErrorPopup;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.UserError;
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
 * @author K. Benedyczak
 */
public class AuthenticatorSetComponent extends VerticalLayout implements ActivationListener
{
	private static final long serialVersionUID = 1L;
	private UnityMessageSource msg;
	private Button authenticateButton;
	private UsernameComponent usernameComponent;
	
	public AuthenticatorSetComponent(Map<String, VaadinAuthentication> authenticators,
			AuthenticatorSet set, UnityMessageSource msg)
	{
		this.msg = msg;
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
		
		addComponent(authenticateButton);
		setComponentAlignment(authenticateButton, Alignment.MIDDLE_CENTER);
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
			List<AuthenticationResult> results = new ArrayList<AuthenticationResult>();
			for (String authenticator: set.getAuthenticators())
			{
				VaadinAuthentication vaadinAuth = authenticators.get(authenticator);
				results.add(vaadinAuth.getAuthenticationResult());
			}
			
			try
			{
				AuthenticationProcessor.processResults(results);
			} catch (AuthenticationException e)
			{
				String error = msg.getMessage(e.getMessage());
				if (usernameComp != null)
					usernameComp.setError(error);
				ErrorPopup.showError(error, "");
			}
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
