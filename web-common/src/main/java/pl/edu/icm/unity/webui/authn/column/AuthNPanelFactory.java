/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import pl.edu.icm.unity.webui.authn.column.AuthenticationOptionsHandler.AuthNOption;

/**
 * Helper interface for producing AuthNPanel components. Thanks to it we can have simple creation of authn 
 * panels in various components and a single centralized creation, which is only place bothering with its many dependencies.
 * 
 * @author K. Benedyczak
 */
public interface AuthNPanelFactory
{
	PrimaryAuthNPanel createRegularAuthnPanel(AuthNOption authnOption);
	PrimaryAuthNPanel createGridCompatibleAuthnPanel(AuthNOption authnOption);
	
}
