/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;

@Tag("div")
abstract class AuthNPanelBase extends Component implements AuthenticationUIController, HasEnabled, HasComponents, HasStyle
{
	protected final VaadinAuthentication.VaadinAuthenticationUI authnUI;
	protected final AuthenticationOptionKey optionId;
	protected final VerticalLayout authenticatorContainer;
	
	protected AuthNPanelBase(VaadinAuthentication.VaadinAuthenticationUI authnUI, AuthenticationOptionKey optionId,
	                         VerticalLayout authenticatorContainer)
	{
		this.authnUI = authnUI;
		this.optionId = optionId;
		this.authenticatorContainer = authenticatorContainer;
		getStyle().set("width", "100%");
	}

	@Override
	public boolean focusIfPossible()
	{
		if (authenticatorContainer.getComponentCount() == 0)
			return false;
		return updateFocus(authenticatorContainer.getComponentAt(0));
	}

	@Override
	public void cancel()
	{
		authnUI.clear();
	}

	@Override
	public AuthenticationOptionKey getAuthenticationOptionId()
	{
		return optionId;
	}
	
	@Override
	public void setEnabled(boolean enabled)
	{
		((HasEnabled)authnUI.getComponent()).setEnabled(enabled);
	}
	
	private boolean updateFocus(Component retrievalComponent)
	{
		if (retrievalComponent instanceof Focusable)
		{
			((Focusable)retrievalComponent).focus();
			return true;
		}
		return false;
	}
}
