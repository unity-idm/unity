/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.utils.ClasspathResourceReader;
import pl.edu.icm.unity.engine.utils.ClasspathResourceReader.NamedJson;

/**
 * Provides system credentials
 * @author P.Piernik
 *
 */
@Component
public class SystemCredentialProvider
{
	public static final String CREDENTIAL_CLASSPATH = "credentials";
	private static final String OVERRIDE_SUFFIX = ".override.json";

	private ApplicationContext applicationContext;
	private Collection<CredentialDefinition> credentials;
	
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_CORE,
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
			Collection<NamedJson> namedJsons = classPathReader
					.readNamedJsons(CREDENTIAL_CLASSPATH);

			Map<String, CredentialDefinition> byName = new LinkedHashMap<>();
			for (NamedJson namedJson : namedJsons)
			{
				if (isOverride(namedJson))
					continue;
				CredentialDefinition credential = toCredential(namedJson);
				if (byName.containsKey(credential.getName()))
				{
					throw new InternalException("Duplicate definition of system credential "
							+ credential.getName());
				}
				LOG.info("Adding system credential '{}'", credential.getName());
				byName.put(credential.getName(), credential);
			}

			for (NamedJson namedJson : namedJsons)
			{
				if (!isOverride(namedJson))
					continue;
				CredentialDefinition credential = toCredential(namedJson);
				LOG.info("Overriding system credential '{}' with '{}'",
						credential.getName(), namedJson.filename);
				byName.put(credential.getName(), credential);
			}

			credentials.addAll(byName.values());
		} catch (Exception e)
		{
			throw new InternalException("Can't load system credentials", e);
		}
	}

	private static boolean isOverride(NamedJson namedJson)
	{
		return namedJson.filename != null && namedJson.filename.endsWith(OVERRIDE_SUFFIX);
	}

	private static CredentialDefinition toCredential(NamedJson namedJson)
	{
		CredentialDefinition credential = new CredentialDefinition(namedJson.json);
		credential.setReadOnly(true);
		return credential;
	}

	public Collection<CredentialDefinition> getSystemCredentials()
	{
		List<CredentialDefinition> copy = new ArrayList<>();
		for (CredentialDefinition c : credentials)
			copy.add(c.clone());
		return copy;
	}
	
}
