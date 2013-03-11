/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.internal.InternalEndpointManagement;
import pl.edu.icm.unity.engine.mock.MockPasswordHandlerFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml", "classpath:META-INF/test-components.xml"})
@ActiveProfiles("test")
public abstract class DBIntegrationTestBase
{
	@Autowired
	protected GroupsManagement groupsMan;
	@Autowired
	protected IdentitiesManagement idsMan;
	@Autowired
	protected AttributesManagement attrsMan;
	@Autowired
	protected ServerManagement serverMan;
	@Autowired
	protected EndpointManagementImpl endpointMan;
	@Autowired
	protected InternalEndpointManagement internalEndpointMan;
	@Autowired
	protected AuthenticationManagement authnMan;
	@Autowired
	protected JettyServer httpServer;
	
	@Before
	public void clear() throws EngineException
	{
		serverMan.resetDatabase();
	}
	
	
	protected void checkArray(Object[] toBeChecked, Object... shouldBeIn)
	{
		for (Object o: shouldBeIn)
		{
			boolean found = false;
			for (Object in: toBeChecked)
			{
				if (in.equals(o)){
					found = true;
					break;
				}
			}
			if (!found)
				fail("No " + o + " was found");
		}
	}
	
	protected Attribute<?> getAttributeByName(List<Attribute<?>> attrs, String name)
	{
		for (Attribute<?> a: attrs)
			if (a.getName().equals(name))
				return a;
		return null;
	}

	protected AttributeType getAttributeTypeByName(List<AttributeType> attrs, String name)
	{
		for (AttributeType a: attrs)
			if (a.getName().equals(name))
				return a;
		return null;
	}
	
	protected void setupAuthn() throws Exception
	{
		CredentialDefinition credDef = new CredentialDefinition(
				MockPasswordHandlerFactory.ID, "credential1", "cred desc");
		credDef.setJsonConfiguration("8");
		authnMan.addCredentialDefinition(credDef);
		
		CredentialRequirements cr = new CredentialRequirements("crMock", "mock cred req", 
				Collections.singleton(credDef.getName()));
		authnMan.addCredentialRequirement(cr);
	}

}
