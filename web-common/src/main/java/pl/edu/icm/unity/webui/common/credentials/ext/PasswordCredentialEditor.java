/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.ext;

import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.PasswordField;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Allows to setup password for password credential.
 * @author K. Benedyczak
 */
public class PasswordCredentialEditor implements CredentialEditor
{
	private UnityMessageSource msg;
	private PasswordField password1;
	private PasswordField password2;

	public PasswordCredentialEditor(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public Component getEditor(String credentialConfiguration)
	{
		password1 = new PasswordField(msg.getMessage("PasswordCredentialEditor.password"));
		password2 = new PasswordField(msg.getMessage("PasswordCredentialEditor.repeatPassword"));
		FormLayout ret = new FormLayout(password1, password2);
		ret.setSpacing(true);
		ret.setMargin(true);
		return ret;
	}

	@Override
	public String getValue() throws IllegalCredentialException
	{
		String p1 = password1.getValue();
		String p2 = password2.getValue();
		if (!p1.equals(p2))
		{
			String err = msg.getMessage("PasswordCredentialEditor.passwordsDoNotMatch");
			password2.setComponentError(new UserError(err));
			throw new IllegalCredentialException(err);
		} else
			password2.setComponentError(null);
		return p1;
	}
}
