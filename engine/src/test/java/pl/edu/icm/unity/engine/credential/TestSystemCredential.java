/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.server.EngineInitialization;

/**
 * 
 * @author P.Piernik
 *
 */
public class TestSystemCredential extends DBIntegrationTestBase
{	
	@Autowired
	private CredentialManagement credMan;
	
	@Autowired
	private CredentialRequirementManagement credReqMan;
	
	
	@Test
	public void shouldListSystemCredential() throws EngineException
	{
		Collection<CredentialDefinition> credentialDefinitions = credMan.getCredentialDefinitions();
		assertThat(credentialDefinitions.isEmpty(), is(false));		
		assertThat(credentialDefinitions.iterator().next().isReadOnly(), is(true));
	}
		
	@Test
	public void shouldBlockAddCredentialWithSystemCredentialName() throws EngineException
	{
		CredentialDefinition toAdd = new CredentialDefinition("password", EngineInitialization.DEFAULT_CREDENTIAL);
		Throwable error = catchThrowable(() -> credMan.addCredentialDefinition(toAdd));
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void shouldNotRemoveSystemCredential() throws EngineException
	{
		Throwable error = catchThrowable(() -> credMan.removeCredentialDefinition(EngineInitialization.DEFAULT_CREDENTIAL));
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
		
	@Test
	public void systemCredentialReqShouldContainsAllCredentials() throws EngineException
	{
		CredentialDefinition toAdd = credMan.getCredentialDefinitions().iterator().next();
		toAdd.setName("new cred");
		toAdd.setReadOnly(false);
		credMan.addCredentialDefinition(toAdd);
		
		Collection<CredentialRequirements> credentialRequirements = credReqMan.getCredentialRequirements();	
		for (CredentialRequirements req : credentialRequirements)
		{
			if (req.getName().equals(SystemAllCredentialRequirements.NAME))
			{
				assertThat(req.getRequiredCredentials().contains("new cred"), is(true));
			}			
		}	
	}
		
}
