/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.UsernameProvider;

import com.vaadin.server.UserError;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

public class UsernameComponent extends HorizontalLayout implements UsernameProvider
{
	private static final long serialVersionUID = 1L;
	private TextField username;
	
	public UsernameComponent(UnityMessageSource msg)
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
	
	public void clear()
	{
		username.setValue("");
	}
}