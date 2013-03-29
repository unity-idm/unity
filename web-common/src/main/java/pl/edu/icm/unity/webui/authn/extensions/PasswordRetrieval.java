/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;

import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Retrieves passwords using a Vaadin widget.
 * 
 * @author K. Benedyczak
 */
public class PasswordRetrieval implements CredentialRetrieval, VaadinAuthentication
{
	private UsernameProvider usernameProvider;
	private PasswordExchange credentialExchange;
	private PasswordField passwordField;
	
	@Override
	public String getBindingName()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
	}

	@Override
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (PasswordExchange) e;
	}

	@Override
	public boolean needsCommonUsernameComponent()
	{
		return true;
	}

	@Override
	public Component getComponent()
	{
		HorizontalLayout container = new HorizontalLayout();
		container.addComponent(new Label("Password: "));
		passwordField = new PasswordField();
		container.addComponent(passwordField);
		return container;
	}

	@Override
	public void setUsernameCallback(UsernameProvider usernameCallback)
	{
		this.usernameProvider = usernameCallback;
	}

	@Override
	public AuthenticationResult getAuthenticationResult()
	{
		String username = usernameProvider.getUsername();
		String password = passwordField.getValue();
		if (username.equals("") && password.equals(""))
		{
			passwordField.setComponentError(new UserError("No value"));
			return new AuthenticationResult(Status.notApplicable, null);
		}
		try
		{
			AuthenticatedEntity authenticatedEntity = credentialExchange.checkPassword(username, password);
			return new AuthenticationResult(Status.success, authenticatedEntity);
		} catch (Exception e)
		{
			passwordField.setComponentError(new UserError("Wrong username or password"));
			return new AuthenticationResult(Status.deny, null);
		}
	}
}










