/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.CancelHandler;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;

import java.util.function.Function;

/**
 * The login component of the 1st factor authentication. Wraps a single Vaadin retrieval UI and connects 
 * it to the authentication screen.
 */
public class FirstFactorAuthNPanel extends AuthNPanelBase implements AuthenticationUIController
{
	private final Function<UnknownRemotePrincipalResult, Dialog> unknownUserDialogProvider;
	private final boolean gridCompatible;
	
	public FirstFactorAuthNPanel(
			CancelHandler cancelHandler,
			Function<UnknownRemotePrincipalResult, Dialog> unknownUserDialogProvider,
			boolean gridCompatible,
			VaadinAuthentication.VaadinAuthenticationUI authnUI,
			AuthenticationOptionKey authnId)
	{
		super(authnUI, authnId, new VerticalLayout());
		this.unknownUserDialogProvider = unknownUserDialogProvider;
		this.gridCompatible = gridCompatible;

		authenticatorContainer.setSizeFull();
		authenticatorContainer.setPadding(false);
		authenticatorContainer.setMargin(false);
		authenticatorContainer.addClassName("u-authn-component");
		add(authenticatorContainer);
		setAuthenticator();
	}

	private void setAuthenticator()
	{
		authenticatorContainer.removeAll();
		Component retrievalComponent = gridCompatible ? authnUI.getGridCompatibleComponent() : authnUI.getComponent();
		authenticatorContainer.add(retrievalComponent);
	}
	
	void showUnknownUserDialog(UnknownRemotePrincipalResult urpResult)
	{
		unknownUserDialogProvider.apply(urpResult).open();
	}	
}
