/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

/**
 * Helper interface for producing AuthNPanel components. Thanks to it we can have simple creation of authn 
 * panels in various components and a single centralized creation, which is only place bothering with its many dependencies.
 * 
 * @author K. Benedyczak
 */
public interface AuthNPanelFactory
{
	FirstFactorAuthNPanel createRegularAuthnPanel(AuthNOption authnOption);
	FirstFactorAuthNPanel createGridCompatibleAuthnPanel(AuthNOption authnOption);
	
}
