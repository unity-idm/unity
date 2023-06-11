/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.authn.authenticators;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Factory for {@link AuthenticatorEditor}
 * @author P.Piernik
 *
 */
public interface AuthenticatorEditorFactory
{
	String getSupportedAuthenticatorType();
	
	AuthenticatorEditor createInstance() throws EngineException;
}
