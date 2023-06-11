/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * Common boilerplate for panels wrapping Vaadin retireval.
 * @author K. Benedyczak
 */
abstract class AuthNPanelBase extends CustomComponent implements AuthenticationUIController
{
	protected final VaadinAuthenticationUI authnUI;
	protected final AuthenticationOptionKey optionId;
	protected final AbstractOrderedLayout authenticatorContainer;
	
	protected AuthNPanelBase(VaadinAuthenticationUI authnUI, AuthenticationOptionKey optionId,
			AbstractOrderedLayout authenticatorContainer)
	{
		this.authnUI = authnUI;
		this.optionId = optionId;
		this.authenticatorContainer = authenticatorContainer;
	}

	@Override
	public boolean focusIfPossible()
	{
		if (authenticatorContainer.getComponentCount() == 0)
			return false;
		return updateFocus(authenticatorContainer.getComponent(0));
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
		super.setEnabled(enabled);
		authnUI.getComponent().setEnabled(enabled);
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
