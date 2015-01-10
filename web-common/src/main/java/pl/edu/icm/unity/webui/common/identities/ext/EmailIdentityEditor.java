/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;

import com.vaadin.server.UserError;
import com.vaadin.ui.TextField;

/**
 * {@link EmailIdentity} editor
 * @author P. Piernik
 */
public class EmailIdentityEditor implements IdentityEditor
{
	private UnityMessageSource msg;
	private TextField field;
	private boolean required;
	
	public EmailIdentityEditor(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(boolean required)
	{
		field = new TextField(msg.getMessage("EmailIdentityEditor.email"));
		field.setRequired(required);
		field.setId("EmailIdentityEditor.email");
		this.required = required;
		return new ComponentsContainer(field);
	}

	@Override
	public String getValue() throws IllegalIdentityValueException
	{
		String username = field.getValue();
		if (username.trim().equals(""))
		{
			if (!required)
				return null;
			String err = msg.getMessage("EmailIdentityEditor.emailEmpty");
			field.setComponentError(new UserError(err));
			throw new IllegalIdentityValueException(err);
		}
		field.setComponentError(null);		
		return username;
	}

	@Override
	public void setDefaultValue(String value)
	{
		field.setValue(value);	
	}
}
