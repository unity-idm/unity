/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.List;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;

/**
 * Customization of the {@link ChipsWithDropdown} for authentication flows
 * selection.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RemoteAuthnProvidersMultiSelection extends ChipsWithDropdown<AuthenticationOptionsSelector>
{
	public RemoteAuthnProvidersMultiSelection(MessageSource msg, AuthenticatorSupportService authenticatorSupport, String caption,
			String description) throws EngineException
	{
		this(msg, caption, description);
		init(authenticatorSupport);
	}
	
	public RemoteAuthnProvidersMultiSelection (MessageSource msg, String caption, String description) throws EngineException
	{
		super(s -> s.getDisplayedNameFallbackToConfigKey(msg), true);
		setCaption(caption);
		setDescription(description);
		setWidth(100, Unit.PERCENTAGE);	
	}
	
	private void init(AuthenticatorSupportService authenticatorSupport) throws EngineException
	{
		List<AuthenticatorInstance> remoteAuthenticators = authenticatorSupport
				.getRemoteAuthenticators(VaadinAuthentication.NAME);

		List<AuthenticationOptionsSelector> authnOptions = Lists.newArrayList();
		for (AuthenticatorInstance authenticator : remoteAuthenticators)
		{
			authnOptions.addAll(authenticator.getAuthnOptionSelectors());
		}
		
		setItems(authnOptions);
	}
	
	@Override
	protected void sortItems(List<AuthenticationOptionsSelector> items)
	{
		items.sort(null);
	}
}
