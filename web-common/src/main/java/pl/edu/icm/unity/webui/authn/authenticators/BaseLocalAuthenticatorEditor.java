/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.authenticators;

import java.util.Collection;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.binding.StringBindingValue;

/**
 * Base for all local authenticators editor. Contains localCredential Combo with binder
 * @author P.Piernik
 *
 */
public class BaseLocalAuthenticatorEditor extends BaseAuthenticatorEditor
{
	protected ComboBox<String> localCredential;
	private Binder<StringBindingValue> localCredentialBinder;
	private Collection<String> allCredentials;

	public BaseLocalAuthenticatorEditor(UnityMessageSource msg, Collection<String> allCredentials)
	{
		super(msg);
		this.allCredentials = allCredentials;
		localCredential = new ComboBox<>();
		localCredential.setCaption(msg.getMessage("BaseLocalAuthenticatorEditor.localCredential"));
		localCredential.setItems(allCredentials);
		localCredential.setEmptySelectionAllowed(false);

		localCredentialBinder = new Binder<>(StringBindingValue.class);
		localCredentialBinder.forField(localCredential).asRequired(msg.getMessage("fieldRequired")).bind("value");
		localCredential.setEmptySelectionAllowed(false);

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
				credential != null ? (allCredentials.isEmpty() ? "" : allCredentials.iterator().next())
						: credential);
		localCredentialBinder.setBean(value);
	}
}
