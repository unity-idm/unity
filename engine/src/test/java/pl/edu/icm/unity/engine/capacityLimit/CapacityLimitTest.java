/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.capacityLimit;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.CapacityLimitManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.capacityLimits.InternalCapacityLimitVerificator;
import pl.edu.icm.unity.engine.identity.IdentityTypeHelper;
import pl.edu.icm.unity.engine.mock.MockEndpoint;
import pl.edu.icm.unity.engine.mock.MockPasswordVerificatorFactory;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.exceptions.CapacityLimitReachedException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.capacityLimit.CapacityLimit;
import pl.edu.icm.unity.types.capacityLimit.CapacityLimitName;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;

/**
 * Tests capacity limit
 * @author P.Piernik
 *
 */
public class CapacityLimitTest extends DBIntegrationTestBase
{
	@Autowired
	private CapacityLimitManagement capacityMan;

	@Autowired
	private IdentityTypeHelper idTypeHelper;

	@Autowired
	private EnquiryManagement enqMan;

	@Autowired
	private InternalCapacityLimitVerificator capacityLimit;

	@Autowired
	private AuthenticatorManagement authenticatorMan;

	@Autowired
	private AuthenticationFlowManagement flowMan;

	@Autowired
	private AuthenticatorsRegistry authenticatorsReg;

	@After
	public void clearLimits()
	{
		capacityLimit.clearCache();
	}

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededExceptionForGroups() throws EngineException
	{
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.Groups, 3));
		try
		{
			groupsMan.addGroup(new Group("/test"));
			groupsMan.addGroup(new Group("/test2"));
		} catch (CapacityLimitReachedException e)
		{
			fail();
		}
		groupsMan.addGroup(new Group("/test3"));
	}

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededExceptionForEntities() throws EngineException
	{
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.Entities,
				groupsMan.getContents("/", GroupContents.MEMBERS).getMembers().size()));
		idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "xx"), EntityState.valid, false);
	}

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededExceptionForIdentities() throws EngineException
	{
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.Identities,
				groupsMan.getContents("/", GroupContents.MEMBERS).getMembers().size()));

		int ids = 0;
		for (GroupMembership m : groupsMan.getContents("/", GroupContents.MEMBERS).getMembers())
		{
			for (Identity id : idsMan.getEntity(new EntityParam(m.getEntityId())).getIdentities())
			{
				if (!idTypeHelper.getTypeDefinition(id.getTypeId()).isDynamic())
				{
					ids++;
				}
			}
		}
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.Identities, ids));
		idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "xx"), EntityState.valid, false);
	}

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededExceptionForAttributes() throws EngineException
	{
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.Attributes, 2));
		Identity addEntity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "xx"), EntityState.valid,
				false);

		aTypeMan.addAttributeType(new AttributeType("attr1", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("attr2", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("attr3", StringAttributeSyntax.ID));
		try
		{

			attrsMan.createAttribute(new EntityParam(addEntity.getEntityId()), new Attribute("attr1",
					StringAttributeSyntax.ID, "/", new ArrayList<String>(Arrays.asList("x"))));
			attrsMan.createAttribute(new EntityParam(addEntity.getEntityId()), new Attribute("attr2",
					StringAttributeSyntax.ID, "/", new ArrayList<String>(Arrays.asList("x"))));
		} catch (CapacityLimitReachedException e)
		{
			fail();
		}
		attrsMan.createAttribute(new EntityParam(addEntity.getEntityId()), new Attribute("attr3",
				StringAttributeSyntax.ID, "/", new ArrayList<String>(Arrays.asList("x"))));
	}

	@Test
	public void shouldThrowLimitExceededExceptionForAttributesValues() throws EngineException
	{
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.AttributesValues, 2));
		Identity addEntity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "xx"), EntityState.valid,
				false);

		aTypeMan.addAttributeType(new AttributeType("attr1", StringAttributeSyntax.ID));

		AttributeType t2 = new AttributeType("attr2", StringAttributeSyntax.ID);
		t2.setMaxElements(10);
		aTypeMan.addAttributeType(t2);
		aTypeMan.addAttributeType(new AttributeType("attr3", StringAttributeSyntax.ID));

		try
		{
			attrsMan.createAttribute(new EntityParam(addEntity.getEntityId()), new Attribute("attr1",
					StringAttributeSyntax.ID, "/", new ArrayList<String>(Arrays.asList("x"))));
			attrsMan.createAttribute(new EntityParam(addEntity.getEntityId()), new Attribute("attr2",
					StringAttributeSyntax.ID, "/", new ArrayList<String>(Arrays.asList("x"))));
		} catch (CapacityLimitReachedException e)
		{
			fail();
		}

		try
		{
			attrsMan.createAttribute(new EntityParam(addEntity.getEntityId()), new Attribute("attr3",
					StringAttributeSyntax.ID, "/", new ArrayList<String>(Arrays.asList("x"))));
			fail();
		} catch (CapacityLimitReachedException e)
		{
			// ok
		}

		try
		{
			attrsMan.setAttribute(new EntityParam(addEntity.getEntityId()),
					new Attribute("attr2", StringAttributeSyntax.ID, "/",
							new ArrayList<String>(Arrays.asList("x", "x2"))));
			fail();
		} catch (CapacityLimitReachedException e)
		{
			// ok
		}
	}

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededExceptionForAttributesValueSize() throws EngineException
	{

		Identity addEntity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "xx"), EntityState.valid,
				false);

		aTypeMan.addAttributeType(new AttributeType("attr1", StringAttributeSyntax.ID));
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.AttributeValueSize, 2));
		attrsMan.createAttribute(new EntityParam(addEntity.getEntityId()), new Attribute("attr1",
				StringAttributeSyntax.ID, "/", new ArrayList<String>(Arrays.asList("xxx"))));
	}

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededExceptionForAttributeTypeValueSize() throws EngineException
	{
		StringAttributeSyntax syntax = new StringAttributeSyntax();
		syntax.setMaxLength(100);
		AttributeType type1 = new AttributeType("at1", StringAttributeSyntax.ID);
		type1.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.AttributeValueSize, 50));
		aTypeMan.addAttributeType(type1);
	}

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededExceptionForEndpoints() throws EngineException
	{

		AuthenticationRealm realm = new AuthenticationRealm("Test", "", 10, 10, RememberMePolicy.disallow, 1,
				600);
		realmsMan.addRealm(realm);

		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), "desc",
				new ArrayList<String>(), "", "Test");
		try
		{
			endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/foo", cfg);
		} catch (CapacityLimitReachedException e)
		{
			fail();
		}

		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.Endpoints, 1));
		endpointMan.deploy(MockEndpoint.NAME, "endpoint2", "/foo", cfg);
	}

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededExceptionForRegistrationForms() throws EngineException
	{
		RegistrationForm form1 = new RegistrationFormBuilder().withName("reg1")
				.withDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.build();
		RegistrationForm form2 = new RegistrationFormBuilder().withName("reg2")
				.withDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.build();
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.RegistrationForms, 1));
		try
		{
			registrationsMan.addForm(form1);
		} catch (CapacityLimitReachedException e)
		{
			fail();
		}
		registrationsMan.addForm(form2);
	}

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededExceptionForEnquiryForms() throws EngineException
	{
		EnquiryForm form1 = new EnquiryFormBuilder().withName("sticky1").withTargetGroups(new String[] { "/" })
				.withType(EnquiryType.STICKY).build();
		EnquiryForm form2 = new EnquiryFormBuilder().withName("sticky2").withTargetGroups(new String[] { "/" })
				.withType(EnquiryType.STICKY).build();

		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.EnquiryForms, 1));

		try
		{
			enqMan.addEnquiry(form1);

		} catch (CapacityLimitReachedException e)
		{
			fail();
		}
		enqMan.addEnquiry(form2);
	}

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededExceptionForAuthenticators() throws EngineException
	{
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg
				.getAuthenticatorTypesByBinding("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		addCredential();
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.Authenticators, 1));

		try
		{
			authenticatorMan.createAuthenticator("auth1", authType.getVerificationMethod(), "bbb",
					"credential1");

		} catch (CapacityLimitReachedException e)
		{
			fail();
		}
		authenticatorMan.createAuthenticator("auth2", authType.getVerificationMethod(), "bbb", "credential1");
	}

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededExceptionForAuthenticationFlows() throws EngineException
	{
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg
				.getAuthenticatorTypesByBinding("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		addCredential();
		authenticatorMan.createAuthenticator("auth1", authType.getVerificationMethod(), "bbb", "credential1");
		
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.AuthenticationFlows, 1));
		try
		{
			flowMan.addAuthenticationFlow(new AuthenticationFlowDefinition("flow1", Policy.NEVER,
					Sets.newHashSet("auth1")));
		} catch (CapacityLimitReachedException e)
		{
			fail();
		}

		flowMan.addAuthenticationFlow(
				new AuthenticationFlowDefinition("flow2", Policy.NEVER, Sets.newHashSet("auth1")));
	}

	private void addCredential() throws EngineException
	{
		CredentialDefinition credDef = new CredentialDefinition(MockPasswordVerificatorFactory.ID, CRED_MOCK);
		credDef.setConfiguration("8");
		insecureCredMan.addCredentialDefinition(credDef);
	}
}
