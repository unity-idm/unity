/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.authenticators;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.elements.StringBindingValue;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.Collection;


public class BaseLocalAuthenticatorEditor extends BaseAuthenticatorEditor
{
	private final Binder<StringBindingValue> localCredentialBinder;
	private final Collection<String> allCredentials;
	protected ComboBox<String> localCredential;

	public BaseLocalAuthenticatorEditor(MessageSource msg, Collection<String> allCredentials)
	{
		super(msg);
		this.allCredentials = allCredentials;
		localCredential = new ComboBox<>();
		localCredential.setItems(allCredentials);
		localCredentialBinder = new Binder<>(StringBindingValue.class);
		localCredentialBinder.forField(localCredential)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind(StringBindingValue::getValue, StringBindingValue::setValue);
	}

	protected String getLocalCredential() throws FormValidationException
	{
		if (localCredentialBinder.validate().hasErrors())
			throw new FormValidationException();
		
		return localCredentialBinder.getBean().getValue();
	}

	protected void setLocalCredential(String credential)
	{
		StringBindingValue value = new StringBindingValue(
				credential == null ? getDefaultLocalCredential()
						: credential);
		localCredentialBinder.setBean(value);
	}
	
	protected String getDefaultLocalCredential()
	{
		return allCredentials.isEmpty() ? "" : allCredentials.iterator().next();
	}
	
	protected boolean init(String defaultName, AuthenticatorDefinition toEdit, boolean forceNameEditable)
	{
		boolean editMode = super.init(defaultName, toEdit, forceNameEditable);
		setLocalCredential(editMode ? toEdit.localCredentialName : null);
		return editMode;	
	}
}
