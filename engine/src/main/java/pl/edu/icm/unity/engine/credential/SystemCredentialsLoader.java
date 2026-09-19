/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.Collection;

import pl.edu.icm.unity.base.authn.CredentialDefinition;

/**
 * Loads the system credential definitions available on the classpath.
 */
public interface SystemCredentialsLoader
{
	Collection<CredentialDefinition> loadCredentials();
}
