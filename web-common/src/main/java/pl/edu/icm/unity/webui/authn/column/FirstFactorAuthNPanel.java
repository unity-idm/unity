/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.function.Function;

import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.UnknownUserDialog;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * The login component of the 1st factor authentication. Wraps a single Vaadin retrieval UI and connects 
 * it to the authentication screen.  
 * 
 * @author K. Benedyczak
 */
public class FirstFactorAuthNPanel extends AuthNPanelBase implements AuthenticationUIController
{
	private final Function<UnknownRemotePrincipalResult, UnknownUserDialog> unknownUserDialogProvider; 
	private final boolean gridCompatible;
	
	public FirstFactorAuthNPanel(
			CancelHandler cancelHandler,
			Function<UnknownRemotePrincipalResult, UnknownUserDialog> unknownUserDialogProvider,
			boolean gridCompatible,
			VaadinAuthenticationUI authnUI,
			AuthenticationOptionKey authnId)
	{
		super(authnUI, authnId, new VerticalLayout());
		this.unknownUserDialogProvider = unknownUserDialogProvider;
		this.gridCompatible = gridCompatible;

		authenticatorContainer.setHeight(100, Unit.PERCENTAGE);
		authenticatorContainer.setWidth(100, Unit.PERCENTAGE);
		authenticatorContainer.setSpacing(false);
		authenticatorContainer.setMargin(false);
		authenticatorContainer.addStyleName("u-authn-component");
		setCompositionRoot(authenticatorContainer);
		setAuthenticator();
	}

	private void setAuthenticator()
	{
		authenticatorContainer.removeAllComponents();
		Component retrievalComponent = gridCompatible ? authnUI.getGridCompatibleComponent() : authnUI.getComponent();
		authenticatorContainer.addComponent(retrievalComponent);
	}
	
	void showUnknownUserDialog(UnknownRemotePrincipalResult urpResult)
	{
		unknownUserDialogProvider.apply(urpResult).show(); 
	}	
}
