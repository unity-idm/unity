/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.event.EventCategory;
import pl.edu.icm.unity.engine.api.initializers.ScriptConfiguration;
import pl.edu.icm.unity.engine.api.initializers.ScriptType;
import pl.edu.icm.unity.engine.scripts.MainGroovyExecutor;
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
	protected MainGroovyExecutor groovyExecutor;
	
	@Test
	public void shouldProvisionCredentialsFromConfiguration() throws EngineException
	{
		// given
		ScriptConfiguration conf = new ScriptConfiguration(ScriptType.groovy, 
				EventCategory.POST_INIT.toString(), 
				"src/test/resources/addCredentialsTest.groovy");
		removeCredentialDefinitions("secured password100");
		int initSizeOfCredentials = credMan.getCredentialDefinitions().size();
		
		// when
		groovyExecutor.run(conf, 
				new Event(EventCategory.POST_INIT.toString(), -1l, new Date()));
		
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
