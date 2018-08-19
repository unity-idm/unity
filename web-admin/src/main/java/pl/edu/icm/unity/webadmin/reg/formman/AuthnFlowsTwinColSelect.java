/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.ui.TwinColSelect;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Customization of the {@link TwinColSelect} for authentication flows
 * selection.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class AuthnFlowsTwinColSelect extends TwinColSelect<String>
{
	public AuthnFlowsTwinColSelect(AuthenticatorSupportManagement authenticatorSupport, String leftCaption,
			String rightCaption, String caption) throws EngineException
	{
		setCaption(caption);
		setLeftColumnCaption(leftCaption);
		setRightColumnCaption(rightCaption);
		setRows(10);
		init(authenticatorSupport);
	}

	private void init(AuthenticatorSupportManagement authenticatorSupport) throws EngineException
	{
		List<AuthenticationFlowDefinition> definitions = authenticatorSupport
				.resolveAllRemoteAuthenticatorFlows(VaadinAuthentication.NAME);
		List<String> flows = definitions.stream()
				.map(AuthenticationFlowDefinition::getName)
				.collect(Collectors.toList());
		setItems(flows);
	}
}
