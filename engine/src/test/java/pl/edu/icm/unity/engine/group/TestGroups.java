/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;


import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.group.GroupsChain;
import pl.edu.icm.unity.engine.api.group.IllegalGroupValueException;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.engine.group.GroupsManagementImpl.PublicChildGroupException;
import pl.edu.icm.unity.engine.group.GroupsManagementImpl.ParentIsPrivateGroupException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;


public class TestGroups extends DBIntegrationTestBase
{
	@Test
	public void testIdTypes() throws Exception
	{
		for (IdentityType idType: idTypeMan.getIdentityTypes())
			System.out.println(idType);
	}
	
	@Test
	public void testGetContentsWithLimitedAuthz() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		ab.setDescription(new I18nString("d-n"));
		groupsMan.addGroup(ab);
		Group ac = new Group("/A/C");
		groupsMan.addGroup(ac);
		
		EntityParam ep = new EntityParam(new IdentityTaV(UsernameIdentity.ID, DEF_USER));
		groupsMan.addMemberFromParent("/A", ep);
		groupsMan.addMemberFromParent("/A/B", ep);
		
		setupUserContext(DEF_USER, null);
		
		GroupContents rootC = groupsMan.getContents("/", GroupContents.EVERYTHING);
		assertThat(rootC.getSubGroups()).hasSize(1);
		assertThat(rootC.getSubGroups().get(0)).isEqualTo("/A");
		assertThat(rootC.getGroup()).isNotNull();
		assertThat(rootC.getMembers()).isNotNull();
		
		GroupContents aC = groupsMan.getContents("/A", GroupContents.EVERYTHING);
		assertThat(aC.getSubGroups()).hasSize(1);
		assertThat(aC.getSubGroups().get(0)).isEqualTo("/A/B");
		assertThat(aC.getGroup()).isNotNull();
		assertThat(aC.getMembers()).isNotNull();

		GroupContents abC = groupsMan.getContents("/A/B", GroupContents.EVERYTHING);
		assertThat(abC.getGroup()).isNotNull();
		assertThat(abC.getGroup().getDescription()).isEqualTo(new I18nString("d-n"));
	}

	@Test
	public void shouldFailToGetContentsOfGroupWhereIsNotAMember() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		Group ac = new Group("/A/C");
		groupsMan.addGroup(ac);
		
		EntityParam ep = new EntityParam(new IdentityTaV(UsernameIdentity.ID, DEF_USER));
		groupsMan.addMemberFromParent("/A", ep);
		
		setupUserContext(DEF_USER, null);
		
		Throwable error = catchThrowable(() -> groupsMan.getContents("/A/C", GroupContents.EVERYTHING));
		
		assertThat(error).isNotNull().isInstanceOf(AuthorizationException.class);
	}
	
	@Test
	public void shouldNotAddTooLongGroup() throws Exception
	{
		StringBuilder sb = new StringBuilder("/");
		for (int i=0; i<201; i++)
			sb.append("a");
		Group tooBig = new Group(sb.toString());
		
		Throwable error = catchThrowable(() -> groupsMan.addGroup(tooBig));
		
		assertThat(error).isNotNull().isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void shouldNotRemoveRootGroup() throws Exception
	{
		Throwable error = catchThrowable(() -> groupsMan.removeGroup("/", true));
		
		assertThat(error).isNotNull().isInstanceOf(IllegalGroupValueException.class);
	}

	@Test
	public void shouldNotNonEmptyGroupWithoutRecursiveFlag() throws Exception
	{
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		groupsMan.addGroup(ab);
		
		Throwable error = catchThrowable(() -> groupsMan.removeGroup("/A", false));
		
		assertThat(error).isNotNull().isInstanceOf(IllegalGroupValueException.class);
	}
	
	@Test
	public void testCreate() throws Exception
	{
		AttributeType atFoo = new AttributeType("foo", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(atFoo);
		
		Group a = new Group("/A");
		a.setDescription(new I18nString("foo"));
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		groupsMan.addGroup(ab);
		Group ac = new Group("/A/C");
		ac.setDescription(new I18nString("goo"));
		groupsMan.addGroup(ac);
		Group abd = new Group("/A/B/D");
		groupsMan.addGroup(abd);

		AttributeStatement[] statements = new AttributeStatement[2];
		statements[0] = AttributeStatement.getFixedEverybodyStatement(
				StringAttribute.of("foo", "/A", "val1"));
		statements[1] = AttributeStatement.getFixedStatement(
				StringAttribute.of("foo", "/A", "val1"),
				"/A/B", "eattr['foo'] != null");
		a.setAttributeStatements(statements);
		groupsMan.updateGroup("/A", a);
		
		Group root = new Group("/");
		root.setDescription(new I18nString("root desc"));
		groupsMan.updateGroup("/", root);
		
		GroupContents contentRoot = groupsMan.getContents("/", GroupContents.EVERYTHING);
		assertThat(contentRoot.getSubGroups()).hasSize(1);
		assertThat(contentRoot.getSubGroups().get(0)).isEqualTo("/A");
		assertThat(contentRoot.getGroup().getDescription()).isEqualTo(new I18nString("root desc"));
		
		GroupContents contentA = groupsMan.getContents("/A", GroupContents.EVERYTHING);
		assertThat(contentA.getGroup().getDescription()).isEqualTo(new I18nString("foo"));
		assertThat(contentA.getSubGroups()).hasSize(2);
		assertThat(contentA.getSubGroups().contains("/A/B")).isTrue();
		assertThat(contentA.getSubGroups().contains("/A/C")).isTrue();
		assertThat(contentA.getGroup().getAttributeStatements().length).isEqualTo(2);
		AttributeStatement attributeStatement = contentA.getGroup().getAttributeStatements()[0];
		assertThat(AttributeStatement.ConflictResolution.skip).isEqualTo(
				attributeStatement.getConflictResolution());
		assertThat(attributeStatement.getAssignedAttributeName()).isEqualTo("foo");
		assertThat(attributeStatement.getFixedAttribute()).isNotNull();
		assertThat(attributeStatement.getFixedAttribute().getValues().get(0).toString()).isEqualTo("val1");
		
		GroupContents contentAB = groupsMan.getContents("/A/B", GroupContents.EVERYTHING);
		assertThat(contentAB.getSubGroups()).hasSize(1);
		assertThat(contentAB.getSubGroups().get(0)).isEqualTo("/A/B/D");

		GroupContents contentAC = groupsMan.getContents("/A/C", GroupContents.EVERYTHING);
		assertThat(contentAC.getSubGroups()).isEmpty();
		assertThat(contentAC.getGroup().getDescription()).isEqualTo(new I18nString("goo"));
		
		groupsMan.removeGroup("/A/B/D", false);
		contentAB = groupsMan.getContents("/A/B", GroupContents.EVERYTHING);
		assertThat(contentAB.getSubGroups()).isEmpty();
		
		groupsMan.removeGroup("/A", true);
		contentRoot = groupsMan.getContents("/", GroupContents.EVERYTHING);
		assertThat(contentRoot.getSubGroups()).isEmpty();
	}
	
	@Test
	public void getChildrenReturnsAll() throws Exception
	{
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		groupsMan.addGroup(ab);
		Group ac = new Group("/A/C");
		groupsMan.addGroup(ac);
		Group abd = new Group("/A/B/D");
		groupsMan.addGroup(abd);

		Set<String> rootChildren = groupsMan.getChildGroups("/");
		
		assertThat(rootChildren).hasSize(5);
		assertThat(rootChildren.contains("/")).isEqualTo(true);
		assertThat(rootChildren.contains("/A")).isEqualTo(true);
		assertThat(rootChildren.contains("/A/B")).isEqualTo(true);
		assertThat(rootChildren.contains("/A/C")).isEqualTo(true);
		assertThat(rootChildren.contains("/A/B/D")).isEqualTo(true);

		Set<String> abChildren = groupsMan.getChildGroups("/A/B");
		
		assertThat(abChildren).hasSize(2);
		assertThat(abChildren.contains("/A/B")).isEqualTo(true);
		assertThat(abChildren.contains("/A/B/D")).isEqualTo(true);
	}
	
	@Test 
	public void isPresentGroup() throws Exception
	{
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		assertThat(groupsMan.isPresent("/A")).isEqualTo(true);
		assertThat(groupsMan.isPresent("/B")).isEqualTo(false);
	}
	
	@Test
	public void shouldReturnAllGroupsByWildcard() throws EngineException
	{
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		groupsMan.addGroup(ab);

		List<Group> groups = groupsMan.getGroupsByWildcard("/**");
		
		assertThat(groups).contains(a, ab, new Group("/"));
		assertThat(groups).hasSize(3);
	}

	@Test
	public void shouldReturnFilteredGroupsByWildcard() throws EngineException
	{
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		groupsMan.addGroup(ab);
		Group c = new Group("/C");
		groupsMan.addGroup(c);

		List<Group> groups = groupsMan.getGroupsByWildcard("/A/**");
		
		assertThat(groups).contains(a, ab);
		assertThat(groups).hasSize(2);
	}
	
	@Test
	public void shouldForbidChangeAccessModeToCloseWhenChildGroupIsOpen() throws EngineException
	{	
		Group g = new Group("/");
		g.setPublic(true);
		groupsMan.updateGroup("/", g);
		
		Group parent = new Group("/Parent");
		parent.setPublic(true);
		groupsMan.addGroup(parent);
		
		Group child1 = new Group("/Parent/Child1");
		child1.setPublic(true);
		groupsMan.addGroup(child1);
		
		Group child2 = new Group("/Parent/Child2");
		child2.setPublic(true);
		groupsMan.addGroup(child2);
		
		parent.setPublic(false);
		
		Throwable exception = catchThrowable(
				() -> groupsMan.updateGroup(parent.getName(), parent));
		assertExceptionType(exception, PublicChildGroupException.class);
	}

	@Test
	public void shouldForbidChangeAccessModeToOpenWhenParentGroupIsClose() throws EngineException
	{
		Group g = new Group("/");
		g.setPublic(true);
		groupsMan.updateGroup("/", g);
		
		Group parent = new Group("/Parent");
		parent.setPublic(false);
		groupsMan.addGroup(parent);	
		
		Group child1 = new Group("/Parent/Child1");
		child1.setPublic(false);
		groupsMan.addGroup(child1);	
		
		child1.setPublic(true);
		
		Throwable exception = catchThrowable(
				() -> groupsMan.updateGroup(child1.getName(), child1));
		assertExceptionType(exception, ParentIsPrivateGroupException.class);
	}
	
	@Test
	public void shouldGetGroupChain() throws EngineException
	{
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		groupsMan.addGroup(ab);
		Group abd = new Group("/A/B/D");
		groupsMan.addGroup(abd);
		
		GroupsChain groupChain = groupsMan.getGroupsChain("/A/B/D");	
		assertThat(groupChain.groups).hasSize(4);
		assertThat(groupChain.groups.get(0).getPathEncoded()).isEqualTo("/");	
		assertThat(groupChain.groups.get(1).getPathEncoded()).isEqualTo("/A");	
		assertThat(groupChain.groups.get(2).getPathEncoded()).isEqualTo("/A/B");	
		assertThat(groupChain.groups.get(3).getPathEncoded()).isEqualTo("/A/B/D");	
	}

	protected void assertExceptionType(Throwable exception, Class<?> type)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(type);
	}
}
