/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.utils.ClasspathResourceReader;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Provides system credentials
 * @author P.Piernik
 *
 */
@Component
public class SystemCredentialProvider
{
	public static final String CREDENTIAL_CLASSPATH = "credentials";

	private ApplicationContext applicationContext;
	private Collection<CredentialDefinition> credentials;
	
	private static final Logger LOG = Log.getLogger(Log.U_SERVER,
			SystemCredentialProvider.class);

	@Autowired
	public SystemCredentialProvider(ApplicationContext applicationContext)
			
	{
		this.applicationContext = applicationContext;
		this.credentials = new ArrayList<>();
		loadCredentials();
	}

	private void loadCredentials()
	{

		ClasspathResourceReader classPathReader = new ClasspathResourceReader(
				applicationContext);
		try
		{
			Collection<ObjectNode> jsons = classPathReader
					.readJsons(CREDENTIAL_CLASSPATH);

			if (jsons.isEmpty())
			{
				LOG.debug("Directory with system credentials is empty");
				return;
			}
			for (ObjectNode json : jsons)
			{
				CredentialDefinition credential = new CredentialDefinition(json);
				credential.setReadOnly(true);	
				checkCredential(credential);
				LOG.info("Adding system credential '{}'", credential.getName());
				credentials.add(credential);
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't load system credentials", e);
		}

	}

	public Collection<CredentialDefinition> getSystemCredentials()
	{
		List<CredentialDefinition> copy = new ArrayList<>();
		for (CredentialDefinition c : credentials)
			copy.add(c.clone());
		return copy;
	}
	
	private void checkCredential(CredentialDefinition cred) throws EngineException
	{
		if (credentials.stream().map(c -> c.getName()).collect(Collectors.toSet()).contains(cred.getName()))
		{
			throw new InternalException("Duplicate definition of system credential " + cred.getName());
		}
	}

}
