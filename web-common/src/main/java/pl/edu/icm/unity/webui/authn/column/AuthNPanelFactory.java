/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * Helper interface for producing AuthNPanel components. Thanks to it we can have simple creation of authn 
 * panels in various components and a single centralized creation, which is only place bothering with its many dependencies.
 * 
 * @author K. Benedyczak
 */
public interface AuthNPanelFactory
{
	PrimaryAuthNPanel createRegularAuthnPanel(AuthenticationOption option, VaadinAuthenticationUI ui);
	PrimaryAuthNPanel createGridCompatibleAuthnPanel(AuthenticationOption option, VaadinAuthenticationUI ui);
	
}
