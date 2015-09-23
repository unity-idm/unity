/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;

import com.vaadin.server.UserError;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;

/**
 * {@link EmailIdentity} editor
 * @author P. Piernik
 */
public class EmailIdentityEditor implements IdentityEditor
{
	private UnityMessageSource msg;
	private TextField field;
	private CheckBox confirmed;
	private boolean required;
	private boolean adminMode;
	
	public EmailIdentityEditor(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(boolean required, boolean adminMode)
	{
		field = new TextField(new EmailIdentity().getHumanFriendlyName(msg) + ":");
		field.setRequired(required);
		this.required = required;
		this.adminMode = adminMode;

		ComponentsContainer ret = new ComponentsContainer(field);
		
		if (adminMode)
		{
			confirmed = new CheckBox(msg.getMessage(
					"VerifiableEmailAttributeHandler.confirmedCheckbox"));
			ret.add(confirmed);
		}
		return ret;

	}

	@Override
	public IdentityParam getValue() throws IllegalIdentityValueException
	{
		String emailVal = field.getValue();
		if (emailVal.trim().equals(""))
		{
			if (!required)
				return null;
			String err = msg.getMessage("EmailIdentityEditor.emailEmpty");
			field.setComponentError(new UserError(err));
			throw new IllegalIdentityValueException(err);
		}
		field.setComponentError(null);
		
		VerifiableEmail ve = new VerifiableEmail(emailVal);
		if (adminMode)
			ve.setConfirmationInfo(new ConfirmationInfo(confirmed.getValue()));
		return EmailIdentity.toIdentityParam(ve, null, null);
	}

	@Override
	public void setDefaultValue(IdentityParam value)
	{
		VerifiableEmail ve = EmailIdentity.fromIdentityParam(value);
		field.setValue(ve.getValue());
		if (adminMode)
			confirmed.setValue(ve.isConfirmed());
	}

	@Override
	public void setLabel(String value)
	{
		field.setCaption(value);
	}
}
