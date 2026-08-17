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
import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.utils.ClasspathResourceReader;
import pl.edu.icm.unity.engine.utils.ClasspathResourceReader.NamedJson;

/**
 * Shared classpath reading logic used by {@link SystemCredentialsLoader} implementations.
 */
class SystemCredentialFiles
{
	static final String CREDENTIAL_CLASSPATH = "credentials";
	static final String TEST_SUFFIX = "-test.json";

	private SystemCredentialFiles()
	{
	}

	/**
	 * A classpath file paired with the credential parsed from it, so that later processing
	 * (deduplication, overriding, logging) never has to re-parse the source JSON.
	 */
	record CredentialFile(String filename, CredentialDefinition credential)
	{
		String name()
		{
			return credential.getName();
		}
	}

	static Collection<CredentialFile> readAllCredentialFiles(ApplicationContext applicationContext)
	{
		ClasspathResourceReader classPathReader = new ClasspathResourceReader(applicationContext);
		try
		{
			List<CredentialFile> files = new ArrayList<>();
			for (NamedJson namedJson : classPathReader.readNamedJsons(CREDENTIAL_CLASSPATH))
				files.add(new CredentialFile(namedJson.filename, toCredential(namedJson)));
			return files;
		} catch (Exception e)
		{
			throw new InternalException("Can't load system credentials", e);
		}
	}

	/**
	 * @return the non test-only files, keyed by the credential name they define
	 */
	static Map<String, CredentialFile> readBaseCredentialFiles(Collection<CredentialFile> files)
	{
		Map<String, CredentialFile> byName = new LinkedHashMap<>();
		for (CredentialFile file : files)
		{
			if (isTestOnly(file))
				continue;
			if (byName.containsKey(file.name()))
			{
				throw new InternalException("Duplicate definition of system credential " + file.name());
			}
			byName.put(file.name(), file);
		}
		return byName;
	}

	static boolean isTestOnly(CredentialFile file)
	{
		return file.filename() != null && file.filename().endsWith(TEST_SUFFIX);
	}

	private static CredentialDefinition toCredential(NamedJson namedJson)
	{
		CredentialDefinition credential = new CredentialDefinition(namedJson.json);
		credential.setReadOnly(true);
		return credential;
	}

	/**
	 * Builds the final credential definitions from the files that actually ended up in
	 * {@code byName}, logging exactly those files.
	 */
	static Collection<CredentialDefinition> toCredentials(Logger log, Map<String, CredentialFile> byName)
	{
		List<CredentialDefinition> credentials = new ArrayList<>();
		for (CredentialFile file : byName.values())
		{
			log.info("Adding system credential '{}' from '{}'", file.name(), file.filename());
			credentials.add(file.credential());
		}
		return credentials;
	}
}
