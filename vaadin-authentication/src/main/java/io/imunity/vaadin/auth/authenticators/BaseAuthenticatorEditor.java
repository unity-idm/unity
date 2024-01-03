/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.authenticators;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import io.imunity.vaadin.elements.StringBindingValue;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.common.FormValidationException;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;


public class BaseAuthenticatorEditor
{
	private final Binder<StringBindingValue> nameBinder;
	protected TextField name;
	protected MessageSource msg;

	public BaseAuthenticatorEditor(MessageSource msg)
	{
		this.msg = msg;
		name = new TextField();
		name.setWidth(TEXT_FIELD_MEDIUM.value());
		nameBinder = new Binder<>(StringBindingValue.class);
		nameBinder.forField(name)
				.withValidator((value, context) ->
				{
					if (value != null && value.contains(" "))
						return ValidationResult.error(msg.getMessage("NoSpaceValidator.noSpace"));
					return ValidationResult.ok();
				}
				).asRequired(msg.getMessage("fieldRequired"))
				.bind(StringBindingValue::getValue, StringBindingValue::setValue);
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
		boolean editMode = toEdit != null && !toEdit.id.isBlank();
		setName(editMode ? toEdit.id : defaultName);
		name.setReadOnly(editMode && !forceNameEditable);
		name.focus();
		return editMode;
	}
}
