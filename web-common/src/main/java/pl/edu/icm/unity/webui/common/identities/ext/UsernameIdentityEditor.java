/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;

/**
 * {@link UsernameIdentity} editor
 * @author K. Benedyczak
 */
public class UsernameIdentityEditor implements IdentityEditor
{
	private UnityMessageSource msg;
	private TextField field;
	
	public UsernameIdentityEditor(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public Component getEditor()
	{
		field = new TextField(msg.getMessage("UsernameIdentityEditor.username"));
		
		FormLayout hl = new FormLayout();
		hl.setSpacing(true);
		hl.addComponents(field);
		hl.setMargin(true);
		return hl;
	}

	@Override
	public String getValue() throws IllegalIdentityValueException
	{
		String username = field.getValue();
		if (username.trim().equals(""))
		{
			String err = msg.getMessage("UsernameIdentityEditor.usernameEmpty");
			field.setComponentError(new UserError(err));
			throw new IllegalIdentityValueException(err);
		}
		return username;
	}
}
