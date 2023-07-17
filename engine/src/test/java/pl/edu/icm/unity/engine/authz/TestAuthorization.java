/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;


public class TestAuthorization extends DBIntegrationTestBase
{
	@Autowired
	private InternalAuthorizationManager underTest;
	
	@Test
	public void shouldNotComplainWhenCheckingAgainstUnknownGroup() throws Exception
	{
		// given
		addRegularUser();
		setupUserContext("user1", null);
		
		// when
		Throwable error = catchThrowable(() -> underTest.checkAuthorization("/unknown", AuthzCapability.readInfo));
		
		// then
		assertThat(error).isNull();
	}
	
	private void setAdminsRole(String role) throws Exception
	{
		Attribute roleAt = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/", role);
		EntityParam adminEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "admin"));
		insecureAttrsMan.setAttribute(adminEntity, roleAt);
	}

	private EntityParam addRegularUser() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "user1");
		Identity added = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());
		attrsMan.createAttribute(entity, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/", InternalAuthorizationManagerImpl.USER_ROLE));
		return entity;
	}
	
	@Test
	public void shouldNotAllowContentsManagerToResetDB() throws Exception
	{
		setAdminsRole(InternalAuthorizationManagerImpl.CONTENTS_MANAGER_ROLE);
		
		Throwable error = catchThrowable(() -> serverMan.resetDatabase());
		
		assertThat(error).isInstanceOf(AuthorizationException.class);
	}	

	@Test
	public void shouldNotAllowUserToResetDB() throws Exception
	{
		addRegularUser();
		setupUserContext("user1", null);

		Throwable error = catchThrowable(() -> serverMan.resetDatabase());
		
		assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void shouldNotAllowOwnerOfSysManRoleInNonRootGroupToResetDB() throws Exception
	{
		EntityParam entity = addRegularUser();
		setupUserContext("admin", null);
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addMemberFromParent("/A", entity);
		attrsMan.removeAttribute(entity, "/", RoleAttributeTypeProvider.AUTHORIZATION_ROLE);
		
		attrsMan.createAttribute(entity, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/A", InternalAuthorizationManagerImpl.SYSTEM_MANAGER_ROLE));
		setupUserContext("user1", null);
		
		Throwable error = catchThrowable(() -> serverMan.resetDatabase());
		
		assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	
	@Test
	public void shouldNotAllowUserToAddGroup() throws Exception
	{
		addRegularUser();
		setupUserContext("user1", null);

		Throwable error = catchThrowable(() -> groupsMan.addGroup(new Group("/A")));
		
		assertThat(error).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void shouldAllowUserToGetOwnedAttributes() throws Exception
	{
		EntityParam entity = addRegularUser();
		setupUserContext("user1", null);

		Throwable error = catchThrowable(() -> attrsMan.getAttributes(entity, "/", null));
		
		assertThat(error).isNull();
	}

	
	@Test
	public void shouldNotAllowForSettingAnAttributeWithOutdatedCredential() throws Exception
	{
		EntityParam entity = addRegularUser();
		setupUserContext("admin", EngineInitialization.DEFAULT_CREDENTIAL);
		
		Throwable error = catchThrowable(() -> attrsMan.setAttribute(entity, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
					"/", InternalAuthorizationManagerImpl.INSPECTOR_ROLE)));
		
		assertThat(error).isInstanceOf(AuthorizationException.class);
	}
	
	
	/*
	@Test
	public void test() throws Exception
	{
		setAdminsRole(AuthorizationManagerImpl.CONTENTS_MANAGER_ROLE);
		
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "user1");
		Identity added = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());
		attrsMan.createAttribute(entity, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/", AuthorizationManagerImpl.USER_ROLE));
		setupUserContext("user1", null);

		
		setupUserContext("admin", null);
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addMemberFromParent("/A", entity);
		attrsMan.removeAttribute(entity, "/", RoleAttributeTypeProvider.AUTHORIZATION_ROLE);
		
		attrsMan.createAttribute(entity, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/A", AuthorizationManagerImpl.SYSTEM_MANAGER_ROLE));
		setupUserContext("user1", null);
	}
*/
	
	@Test
	public void shouldAllowUserWithSysManInGroupToAddSubGroup() throws Exception
	{
		EntityParam entity = addRegularUser();
		setupUserContext("admin", null);
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addMemberFromParent("/A", entity);
		attrsMan.createAttribute(entity, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/A", InternalAuthorizationManagerImpl.SYSTEM_MANAGER_ROLE));
		
		setupUserContext("user1", null);
		Throwable error = catchThrowable(() -> groupsMan.addGroup(new Group("/A/B")));
		
		assertThat(error).isNull();
	}
	
	@Test
	public void shouldAllowUserWithHigherRoleInParentGroupToRemoveGroup() throws Exception
	{
		EntityParam entity = addRegularUser();
		setupUserContext("admin", null);
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/A/B"));
		groupsMan.addMemberFromParent("/A", entity);
		attrsMan.setAttribute(entity, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/", InternalAuthorizationManagerImpl.CONTENTS_MANAGER_ROLE));
		attrsMan.createAttribute(entity, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/A", InternalAuthorizationManagerImpl.ANONYMOUS_ROLE));

		setupUserContext("user1", null);
		
		Throwable error = catchThrowable(() -> groupsMan.removeGroup("/A/B", true));
		
		assertThat(error).isNull();
	}
	
	@Test
	public void shouldAllowUserWithHigherRoleInParentGroupToAddGroup() throws Exception
	{
		EntityParam entity = addRegularUser();
		setupUserContext("admin", null);
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/A/G"));
		groupsMan.addMemberFromParent("/A", entity);
		groupsMan.addMemberFromParent("/A/G", entity);
		attrsMan.setAttribute(entity, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/A", InternalAuthorizationManagerImpl.CONTENTS_MANAGER_ROLE));
		attrsMan.createAttribute(entity, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE,
				"/A/G", InternalAuthorizationManagerImpl.ANONYMOUS_ROLE));

		setupUserContext("user1", null);
		Throwable error = catchThrowable(() -> groupsMan.addGroup(new Group("/A/G/Z")));
		
		assertThat(error).isNull();
	}

	
	@Test
	public void shouldNotAllowUserWithoutRoleToAddGroup() throws Exception
	{
		addRegularUser();
		setupUserContext("user1", null);

		Throwable error = catchThrowable(() -> groupsMan.addGroup(new Group("/B")));
		
		assertThat(error).isInstanceOf(AuthorizationException.class).hasMessageContaining("addGroup");
	}

}
