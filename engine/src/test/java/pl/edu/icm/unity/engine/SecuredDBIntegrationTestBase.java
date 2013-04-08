/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.internal.AttributeStatementsCleaner;
import pl.edu.icm.unity.engine.internal.InternalEndpointManagement;
import pl.edu.icm.unity.engine.mock.MockPasswordVerificatorFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.authn.IdentityResolver;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.DescribedObject;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml", "classpath:META-INF/test-components.xml"})
@ActiveProfiles("test")
public abstract class SecuredDBIntegrationTestBase
{
	@Autowired
	protected GroupsManagement groupsMan;
	@Autowired
	protected IdentitiesManagement idsMan;
	@Autowired
	protected AttributesManagement attrsMan;
	@Autowired
	@Qualifier("insecure")
	protected AttributesManagement insecureAttrsMan;
	@Autowired
	protected ServerManagement serverMan;
	@Autowired
	@Qualifier("insecure")
	protected ServerManagement insecureServerMan;
	@Autowired
	protected EndpointManagement endpointMan;
	@Autowired
	protected InternalEndpointManagement internalEndpointMan;
	@Autowired
	protected IdentityResolver identityResolver;
	@Autowired
	protected AuthenticationManagement authnMan;
	@Autowired
	protected JettyServer httpServer;
	@Autowired 
	protected SystemAttributeTypes systemAttributeTypes;
	@Autowired
	protected AttributeStatementsCleaner statementsCleaner;
	
	@Before
	public void clear() throws EngineException
	{
		insecureServerMan.resetDatabase();
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
	
	protected Attribute<?> getAttributeByName(Collection<Attribute<?>> attrs, String name)
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

	protected <T extends DescribedObject> T getDescObjectByName(Collection<T> objs, String name)
	{
		for (T a: objs)
			if (a.getName().equals(name))
				return a;
		return null;
	}
	
	protected IdentityType getIdentityTypeByName(Collection<IdentityType> objs, String name)
	{
		for (IdentityType a: objs)
			if (a.getIdentityTypeProvider().getId().equals(name))
				return a;
		return null;
	}
	
	protected void setupMockAuthn() throws Exception
	{
		CredentialDefinition credDef = new CredentialDefinition(
				MockPasswordVerificatorFactory.ID, "credential1", "cred desc");
		credDef.setJsonConfiguration("8");
		authnMan.addCredentialDefinition(credDef);
		
		CredentialRequirements cr = new CredentialRequirements("crMock", "mock cred req", 
				Collections.singleton(credDef.getName()));
		authnMan.addCredentialRequirement(cr);
	}
}
