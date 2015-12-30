/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;


public class TestAuthorization extends DBIntegrationTestBase
{
	private void setAdminsRole(String role) throws Exception
	{
		EnumAttribute roleAt = new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE,
				"/", AttributeVisibility.local, role);
		EntityParam adminEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "admin"));
		insecureAttrsMan.setAttribute(adminEntity, roleAt, true);
	}
	
	@Test
	public void test() throws Exception
	{
		setAdminsRole(AuthorizationManagerImpl.CONTENTS_MANAGER_ROLE);
		try
		{
			//tests standard deny
			serverMan.resetDatabase();
			fail("reset db possible for contents man");
		} catch(AuthorizationException e) {}
		
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "user1");
		Identity added = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
		EntityParam entity = new EntityParam(added.getEntityId());
		attrsMan.setAttribute(entity, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE,
				"/", AttributeVisibility.local, AuthorizationManagerImpl.USER_ROLE), false);
		setupUserContext("user1", false);
		try
		{
			//tests standard deny
			serverMan.resetDatabase();
			fail("reset db possible for user");
		} catch(AuthorizationException e) {}
		try
		{
			//tests standard deny
			groupsMan.addGroup(new Group("/A"));
			fail("addGrp possible for user");
		} catch(AuthorizationException e) {}
		
		//tests self access
		attrsMan.getAttributes(entity, "/", null);
		
		setupUserContext("admin", false);
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addMemberFromParent("/A", entity);
		attrsMan.removeAttribute(entity, "/", SystemAttributeTypes.AUTHORIZATION_ROLE);
		
		attrsMan.setAttribute(entity, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE,
				"/A", AttributeVisibility.local, AuthorizationManagerImpl.SYSTEM_MANAGER_ROLE), false);
		setupUserContext("user1", false);
		try
		{
			//tests standard deny
			serverMan.resetDatabase();
			fail("reset db possible for user");
		} catch(AuthorizationException e) {}
		//tests group authz
		groupsMan.addGroup(new Group("/A/B"));
		//tests searching of the role attribute in the parent group
		groupsMan.removeGroup("/A/B", true);
		
		//test if limited role in subgroup won't be effective (should get roles from the parent)
		groupsMan.addGroup(new Group("/A/G"));
		groupsMan.addMemberFromParent("/A/G", entity);
		attrsMan.setAttribute(entity, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE,
				"/A/G", AttributeVisibility.local, AuthorizationManagerImpl.ANONYMOUS_ROLE), false);
		groupsMan.addGroup(new Group("/A/G/Z"));
		
		
		try
		{
			//tests standard deny
			groupsMan.addGroup(new Group("/B"));
			fail("addGrp possible for no-role");
		} catch(AuthorizationException e) 
		{
			assertThat(e.toString().contains("addGroup"), is(true));
		}
		
		//tests outdated credential
		setupUserContext("admin", false);
		attrsMan.setAttribute(entity, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE,
				"/", AttributeVisibility.local, AuthorizationManagerImpl.USER_ROLE), false);
		setupUserContext("admin", true);
		try
		{
			attrsMan.setAttribute(entity, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE,
					"/", AttributeVisibility.local, AuthorizationManagerImpl.INSPECTOR_ROLE), true);
			fail("set attributes with outdated credential");
		} catch(AuthorizationException e) {}
		
		idsMan.setEntityCredential(entity, EngineInitialization.DEFAULT_CREDENTIAL, 
				new PasswordToken("foo12!~").toJson());
		idsMan.getIdentityTypes();
	}
}
