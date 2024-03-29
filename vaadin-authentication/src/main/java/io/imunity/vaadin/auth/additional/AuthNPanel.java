/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.additional;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.auth.VaadinAuthentication;

/**
 * The login component of the additional authentication. Wraps a single Vaadin retrieval UI and connects 
 * it to the additional authentication dialog.
 */
class AuthNPanel extends VerticalLayout
{
	private final VaadinAuthentication.VaadinAuthenticationUI authnUI;
	private final VerticalLayout authenticatorContainer;

	
	AuthNPanel(VaadinAuthentication.VaadinAuthenticationUI authnUI)
	{
		this.authnUI = authnUI;
		this.authenticatorContainer = new VerticalLayout();

		authenticatorContainer.setHeightFull();
		authenticatorContainer.setWidthFull();
		authenticatorContainer.setPadding(false);
		authenticatorContainer.setMargin(false);
		authenticatorContainer.addClassName("u-authn-component");
		add(authenticatorContainer);
		setAuthenticator();
	}

	private void setAuthenticator()
	{
		authnUI.disableCredentialReset();
		Component retrievalComponent = authnUI.getComponent();
		authenticatorContainer.add(retrievalComponent);
	}
	
	void focusIfPossible()
	{
		if (authenticatorContainer.getComponentCount() > 0)
			updateFocus(authenticatorContainer.getComponentAt(0));
	}

	private void updateFocus(Component retrievalComponent)
	{
		if (retrievalComponent instanceof Focusable)
			((Focusable<?>)retrievalComponent).focus();
	}
}
