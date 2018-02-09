/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.IdentityTypesManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.endpoint.InternalEndpointManagement;
import pl.edu.icm.unity.engine.mock.MockPasswordVerificatorFactory;
import pl.edu.icm.unity.engine.server.JettyServer;
import pl.edu.icm.unity.types.NamedObject;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityType;

@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
public abstract class SecuredDBIntegrationTestBase
{
	protected static final int DEF_ATTRS = 1; //Credential requirement attribute (in / group)
	public static final String CR_MOCK = "crMock";
	public static final String CRED_MOCK = "credential1";
	
	@Autowired
	protected GroupsManagement groupsMan;
	@Autowired
	protected IdentityTypesManagement idTypeMan;
	@Autowired
	protected EntityManagement idsMan;
	@Autowired
	protected EntityCredentialManagement eCredMan;
	@Autowired
	protected AttributeClassManagement acMan;
	@Autowired
	protected AttributesManagement attrsMan;
	@Autowired
	protected AttributeTypeManagement aTypeMan;
	@Autowired
	@Qualifier("insecure")
	protected AttributesManagement insecureAttrsMan;
	@Autowired
	protected ServerManagement serverMan;
	@Autowired
	@Qualifier("insecure")
	protected ServerManagement insecureServerMan;
	@Autowired
	protected PreferencesManagement preferencesMan;
	@Autowired
	protected EndpointManagement endpointMan;
	@Autowired
	protected RegistrationsManagement registrationsMan;
	@Autowired
	protected InternalEndpointManagement internalEndpointMan;
	@Autowired
	protected IdentityResolver identityResolver;
	@Autowired
	@Qualifier("insecure")
	protected CredentialManagement insecureCredMan;
	@Autowired
	protected CredentialManagement credMan;
	@Autowired
	protected CredentialRequirementManagement credReqMan;
	@Autowired
	@Qualifier("insecure")
	protected CredentialRequirementManagement insecureCredReqMan;
	@Autowired
	protected NotificationsManagement notMan;
	@Autowired
	protected JettyServer httpServer;
	@Autowired
	protected RealmsManagement realmsMan;
	@Autowired
	protected MessageTemplateManagement messageTemplateMan;
	
	@Before
	public void clear() throws Exception
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
	
	protected <T extends Attribute> T getAttributeByName(Collection<T> attrs, String name)
	{
		for (T a: attrs)
			if (a.getName().equals(name))
				return a;
		return null;
	}

	protected AttributeType getAttributeTypeByName(Collection<AttributeType> attrs, String name)
	{
		for (AttributeType a: attrs)
			if (a.getName().equals(name))
				return a;
		return null;
	}

	protected <T extends NamedObject> T getDescObjectByName(Collection<T> objs, String name)
	{
		for (T a: objs)
			if (a.getName().equals(name))
				return a;
		return null;
	}
	
	protected IdentityType getIdentityTypeByName(Collection<IdentityType> objs, String name)
	{
		for (IdentityType a: objs)
			if (a.getIdentityTypeProvider().equals(name))
				return a;
		return null;
	}

	protected Identity getIdentityByType(List<Identity> objs, String type)
	{
		for (Identity a: objs)
			if (a.getTypeId().equals(type))
				return a;
		return null;
	}

	protected Collection<Identity> getIdentitiesByType(List<Identity> objs, String type)
	{
		Set<Identity> ret = new HashSet<>();
		for (Identity a: objs)
			if (a.getTypeId().equals(type))
				ret.add(a);
		return ret;
	}
	
	protected void setupMockAuthn() throws Exception
	{
		CredentialDefinition credDef = new CredentialDefinition(
				MockPasswordVerificatorFactory.ID, CRED_MOCK);
		credDef.setConfiguration("8");
		insecureCredMan.addCredentialDefinition(credDef);
		
		CredentialRequirements cr = new CredentialRequirements(CR_MOCK, "mock cred req", 
				Collections.singleton(credDef.getName()));
		insecureCredReqMan.addCredentialRequirement(cr);
	}
}
