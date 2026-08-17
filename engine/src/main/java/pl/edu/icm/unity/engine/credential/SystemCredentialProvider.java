/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.CredentialDefinition;

/**
 * Provides system credentials
 * @author P.Piernik
 *
 */
@Component
public class SystemCredentialProvider
{
	private final Collection<CredentialDefinition> credentials;

	@Autowired
	public SystemCredentialProvider(SystemCredentialsLoader credentialsLoader)
	{
		this.credentials = credentialsLoader.loadCredentials();
	}

	public Collection<CredentialDefinition> getSystemCredentials()
	{
		List<CredentialDefinition> copy = new ArrayList<>();
		for (CredentialDefinition c : credentials)
			copy.add(c.clone());
		return copy;
	}
	
}
