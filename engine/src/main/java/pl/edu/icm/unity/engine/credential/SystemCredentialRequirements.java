/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

public class SystemCredentialRequirements extends CredentialRequirements
{
	public static final String NAME = "sys:all";
	private CredentialRepository credentialRepo;

	public SystemCredentialRequirements(CredentialRepository credentialRepo, UnityMessageSource msg)
	{
		setName(NAME);
		setReadOnly(true);
		setDescription(msg.getMessage("CredentialRequirements.systemCredentialRequirements.desc"));
		this.credentialRepo = credentialRepo;
	}

	public Set<String> getRequiredCredentials()
	{
		Set<String> res = new HashSet<>();
		try
		{
			res = credentialRepo.getCredentialDefinitions().stream()
					.map(c -> c.getName()).collect(Collectors.toSet());
		} catch (EngineException e)
		{
			throw new InternalException("Cannot get credentials", e);
		}
		return res;
	}
}