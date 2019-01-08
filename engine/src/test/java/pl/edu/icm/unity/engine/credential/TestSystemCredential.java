/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

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
		catchException(credMan).addCredentialDefinition(toAdd);
		assertThat(caughtException(), isA(IllegalArgumentException.class));
	}
	
	@Test
	public void shouldNotRemoveSystemCredential() throws EngineException
	{
		catchException(credMan).removeCredentialDefinition(EngineInitialization.DEFAULT_CREDENTIAL);
		assertThat(caughtException(), isA(IllegalArgumentException.class));
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
