/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.authenticators;

import com.vaadin.data.Binder;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.binding.StringBindingValue;
import pl.edu.icm.unity.webui.common.validators.NoSpaceValidator;

/**
 * Base for all authenticators editors. Contains name field with binder
 * @author P.Piernik
 *
 */
public class BaseAuthenticatorEditor
{
	protected TextField name;
	protected MessageSource msg;
	private Binder<StringBindingValue> nameBinder;

	public BaseAuthenticatorEditor(MessageSource msg)
	{
		this.msg = msg;
		name = new TextField(msg.getMessage("BaseAuthenticatorEditor.name"));
		nameBinder = new Binder<>(StringBindingValue.class);
		nameBinder.forField(name).withValidator(new NoSpaceValidator(msg)).asRequired(msg.getMessage("fieldRequired")).bind("value");
	}

	protected String getName() throws FormValidationException 
	{
		if (nameBinder.validate().hasErrors())
			throw new FormValidationException();
			
		return nameBinder.getBean().getValue();
	}

	protected void setName(String name)
	{
		StringBindingValue value = new StringBindingValue(name);
		nameBinder.setBean(value);
	}
	
	protected boolean init(String defaultName, AuthenticatorDefinition toEdit, boolean forceNameEditable)
	{
		boolean editMode = toEdit != null;
		setName(editMode ? toEdit.id : defaultName);
		name.setReadOnly(editMode && !forceNameEditable);
		name.focus();
		return editMode;
	}
}
