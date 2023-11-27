/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.auth.authenticators;

import pl.edu.icm.unity.base.exceptions.EngineException;


public interface AuthenticatorEditorFactory
{
	String getSupportedAuthenticatorType();
	
	AuthenticatorEditor createInstance() throws EngineException;
}
