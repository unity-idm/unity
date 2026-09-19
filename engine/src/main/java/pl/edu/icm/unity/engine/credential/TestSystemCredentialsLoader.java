/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.credential.SystemCredentialFiles.CredentialFile;

/**
 * Test-only loader of system credentials: starts from the same base set of definitions as
 * {@link DefaultSystemCredentialsLoader}, then additionally reads {@code credentials/*-test.json}
 * files, which override or add credential definitions for test purposes only (e.g. a faster
 * password hashing configuration).
 */
@Component
@Profile(UnityServerConfiguration.PROFILE_TEST)
public class TestSystemCredentialsLoader implements SystemCredentialsLoader
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_CORE, TestSystemCredentialsLoader.class);

	private final ApplicationContext applicationContext;

	@Autowired
	public TestSystemCredentialsLoader(ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
	}

	@Override
	public Collection<CredentialDefinition> loadCredentials()
	{
		Collection<CredentialFile> files = SystemCredentialFiles.readAllCredentialFiles(applicationContext);
		Map<String, CredentialFile> byName = new LinkedHashMap<>(
				SystemCredentialFiles.readBaseCredentialFiles(files));

		for (CredentialFile file : files)
		{
			if (!SystemCredentialFiles.isTestOnly(file))
				continue;
			byName.put(file.name(), file);
		}
		return SystemCredentialFiles.toCredentials(LOG, byName);
	}
}
