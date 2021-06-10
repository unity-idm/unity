/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
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
public class RemoteAuthnProvidersMultiSelection extends ChipsWithDropdown<AuthenticationOptionsSelector>
{
	public RemoteAuthnProvidersMultiSelection(AuthenticatorSupportService authenticatorSupport, String caption,
			String description) throws EngineException
	{
		this(caption, description);
		init(authenticatorSupport);
	}
	
	public RemoteAuthnProvidersMultiSelection (String caption, String description) throws EngineException
	{
		super(AuthenticationOptionsSelector::toStringEncodedSelector, true);
		setCaption(caption);
		setDescription(description);
		setWidth(100, Unit.PERCENTAGE);	
	}
	
	private void init(AuthenticatorSupportService authenticatorSupport) throws EngineException
	{
		List<AuthenticatorInstance> remoteAuthenticators = authenticatorSupport.getRemoteAuthenticators(
				VaadinAuthentication.NAME);
		
		List<AuthenticationOptionsSelector> authnOptions = Lists.newArrayList();
		for (AuthenticatorInstance authenticator : remoteAuthenticators)
		{
			VaadinAuthentication vaadinRetrieval = (VaadinAuthentication) authenticator.getRetrieval();
			//FIXME that should be refactored - we shouldn't create UIs to get a list of available instances
			Collection<VaadinAuthenticationUI> uiInstances = vaadinRetrieval.createUIInstance(
					Context.REGISTRATION, getMockContext());
			if (uiInstances.size() > 1)
			{
				authnOptions.add(AuthenticationOptionsSelector.allForAuthenticator(
						authenticator.getMetadata().getId()));
			}
			
			for (VaadinAuthenticationUI uiInstance : uiInstances)
			{
				String optionKey = uiInstance.getId();
				authnOptions.add(new AuthenticationOptionsSelector(
						authenticator.getMetadata().getId(), 
						optionKey));
			}
		}
	
		setItems(authnOptions);
	}
	
	@Override
	protected void sortItems(List<AuthenticationOptionsSelector> items)
	{
		items.sort(null);
	}
	
	private static AuthenticatorStepContext getMockContext()
	{
		return new AuthenticatorStepContext(null, null, 1);
	}
}
