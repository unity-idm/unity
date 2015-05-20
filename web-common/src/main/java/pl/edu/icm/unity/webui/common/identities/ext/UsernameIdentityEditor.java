/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;

import com.vaadin.server.UserError;
import com.vaadin.ui.TextField;

/**
 * {@link UsernameIdentity} editor
 * @author K. Benedyczak
 */
public class UsernameIdentityEditor implements IdentityEditor
{
	private UnityMessageSource msg;
	private TextField field;
	private boolean required;
	
	public UsernameIdentityEditor(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(boolean required, boolean adminMode)
	{
		field = new TextField(new UsernameIdentity().getHumanFriendlyName(msg) + ":");
		field.setRequired(required);
		field.setId("UsernameIdentityEditor.username");
		this.required = required;
		return new ComponentsContainer(field);
	}

	@Override
	public IdentityParam getValue() throws IllegalIdentityValueException
	{
		String username = field.getValue();
		if (username.trim().equals(""))
		{
			if (!required)
				return null;
			String err = msg.getMessage("UsernameIdentityEditor.usernameEmpty");
			field.setComponentError(new UserError(err));
			throw new IllegalIdentityValueException(err);
		}
		field.setComponentError(null);		
		return new IdentityParam(UsernameIdentity.ID, username);
	}

	@Override
	public void setDefaultValue(String value)
	{
		field.setValue(value);	
	}
	

	@Override
	public void setLabel(String value)
	{
		field.setCaption(value);
	}
}
