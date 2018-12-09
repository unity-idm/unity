/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;

/**
 * Customization of the {@link ChipsWithDropdown} for authentication flows
 * selection.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RemoteAuthnProvidersSelection extends ChipsWithDropdown<AuthenticationOptionKey>
{
	public RemoteAuthnProvidersSelection(AuthenticatorSupportService authenticatorSupport, String leftCaption,
			String rightCaption, String caption, String description) throws EngineException
	{
		super(AuthenticationOptionKey::toGlobalKey, true);
		setCaption(caption);
		setDescription(description);
		setWidth(100, Unit.PERCENTAGE);
		init(authenticatorSupport);
	}
	
	private void init(AuthenticatorSupportService authenticatorSupport) throws EngineException
	{
		List<AuthenticatorInstance> remoteAuthenticators = authenticatorSupport.getRemoteAuthenticators(
				VaadinAuthentication.NAME);
		
		List<AuthenticationOptionKey> authnOptions = Lists.newArrayList();
		for (AuthenticatorInstance authenticator : remoteAuthenticators)
		{
			VaadinAuthentication vaadinRetrieval = (VaadinAuthentication) authenticator.getRetrieval();
			Collection<VaadinAuthenticationUI> uiInstances = vaadinRetrieval.createUIInstance(Context.REGISTRATION);
			for (VaadinAuthenticationUI uiInstance : uiInstances)
			{
				String optionKey = uiInstance.getId();
				authnOptions.add(new AuthenticationOptionKey(
						authenticator.getMetadata().getId(), 
						optionKey));
			}
		}
		setItems(authnOptions);
	}
}
