/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.initializers.ContentInitConf;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
@TestPropertySource(properties = { "unityConfig: src/test/resources/groovyInitializers.conf" })
public class ContentGroovyExecutorTest
{
	@Autowired
	@Qualifier("insecure")
	private CredentialManagement credMan;
	
	@Autowired
	protected ContentGroovyExecutor groovyExecutor;
	
	@Test
	public void shouldProvisionCredentialsFromConfiguration() throws EngineException
	{
		// given
		ContentInitConf conf = ContentInitConf.builder()
				.withFile(new File("src/test/resources/addCredentialsTest.groovy"))
				.withGroovy()
				.build();
		removeCredentialDefinitions("secured password100");
		int initSizeOfCredentials = credMan.getCredentialDefinitions().size();
		
		// when
		groovyExecutor.run(conf);
		
		// then
		Collection<CredentialDefinition> creds = credMan.getCredentialDefinitions();
		assertThat(creds.size(), equalTo(initSizeOfCredentials + 1));
		CredentialDefinition cred = filterCred(creds, "secured password100");
		assertThat(cred.getDescription().getDefaultValue(), equalTo("addCredentialsTest"));
	}

	private void removeCredentialDefinitions(String... names) throws EngineException
	{
		List<String> credNames = Lists.newArrayList(names);
		List<String> toRemove = credMan.getCredentialDefinitions().stream()
			.filter(cred -> credNames.contains(cred.getName()))
			.map(CredentialDefinition::getName)
			.collect(Collectors.toList());
		for (String creToRemove: toRemove)
		{
			credMan.removeCredentialDefinition(creToRemove);
		}
	}
	
	private CredentialDefinition filterCred(Collection<CredentialDefinition> creds, String credName)
	{
		return creds.stream().filter(cred -> credName.equals(cred.getName())).findAny().get();
	}
}
