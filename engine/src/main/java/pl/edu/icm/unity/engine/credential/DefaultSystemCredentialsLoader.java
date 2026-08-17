/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.Collection;

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
 * Production loader of system credentials: reads a single set of credential definitions from the
 * classpath, one file per credential name. No overrides are supported.
 */
@Component
@Profile("!" + UnityServerConfiguration.PROFILE_TEST)
public class DefaultSystemCredentialsLoader implements SystemCredentialsLoader
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_CORE, DefaultSystemCredentialsLoader.class);

	private final ApplicationContext applicationContext;

	@Autowired
	public DefaultSystemCredentialsLoader(ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
	}

	@Override
	public Collection<CredentialDefinition> loadCredentials()
	{
		Collection<CredentialFile> files = SystemCredentialFiles.readAllCredentialFiles(applicationContext);
		return SystemCredentialFiles.toCredentials(LOG, SystemCredentialFiles.readBaseCredentialFiles(files));
	}
}
