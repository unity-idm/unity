/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.dataview.ComboBoxListDataView;
import io.imunity.vaadin.auth.VaadinAuthentication;
import pl.edu.icm.unity.base.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class RemoteAuthnProvidersMultiSelection extends MultiSelectComboBox<AuthenticationOptionsSelector>
{

	public RemoteAuthnProvidersMultiSelection(MessageSource msg, AuthenticatorSupportService authenticatorSupport) throws EngineException
	{
		this(msg);
		init(authenticatorSupport);
	}
	
	public RemoteAuthnProvidersMultiSelection(MessageSource msg)
	{
		setItemLabelGenerator(s -> s.getRepresentationFallbackToConfigKey(msg));
		setWidthFull();
	}
	
	private void init(AuthenticatorSupportService authenticatorSupport) throws EngineException
	{
		List<AuthenticatorInstance> remoteAuthenticators = authenticatorSupport
				.getRemoteAuthenticators(VaadinAuthentication.NAME);

		List<AuthenticationOptionsSelector> authnOptions = new ArrayList<>();
		for (AuthenticatorInstance authenticator : remoteAuthenticators)
		{
			authnOptions.addAll(authenticator.getAuthnOptionSelectors());
		}
		
		setItems(authnOptions);
	}

	@Override
	public ComboBoxListDataView<AuthenticationOptionsSelector> setItems(
			Collection<AuthenticationOptionsSelector> authenticationOptionsSelectors)
	{
		return super.setItems(authenticationOptionsSelectors);
	}
}
