/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.engine.group.GroupsManagementImpl.PublicChildGroupException;
import pl.edu.icm.unity.engine.group.GroupsManagementImpl.ParentIsPrivateGroupException;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;


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
		assertEquals(1, rootC.getSubGroups().size());
		assertEquals("/A", rootC.getSubGroups().get(0));
		assertNotNull(rootC.getGroup());
		assertNotNull(rootC.getMembers());
		
		GroupContents aC = groupsMan.getContents("/A", GroupContents.EVERYTHING);
		assertEquals(1, aC.getSubGroups().size());
		assertEquals("/A/B", aC.getSubGroups().get(0));
		assertNotNull(aC.getGroup());
		assertNotNull(aC.getMembers());

		GroupContents abC = groupsMan.getContents("/A/B", GroupContents.EVERYTHING);
		assertNotNull(abC.getGroup());
		assertEquals(new I18nString("d-n"), abC.getGroup().getDescription());
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
		assertEquals(1, contentRoot.getSubGroups().size());
		assertEquals("/A", contentRoot.getSubGroups().get(0));
		assertEquals(new I18nString("root desc"), contentRoot.getGroup().getDescription());
		
		GroupContents contentA = groupsMan.getContents("/A", GroupContents.EVERYTHING);
		assertEquals(new I18nString("foo"), contentA.getGroup().getDescription());
		assertEquals(2, contentA.getSubGroups().size());
		assertTrue(contentA.getSubGroups().contains("/A/B"));
		assertTrue(contentA.getSubGroups().contains("/A/C"));
		assertEquals(2, contentA.getGroup().getAttributeStatements().length);
		AttributeStatement attributeStatement = contentA.getGroup().getAttributeStatements()[0];
		assertEquals(AttributeStatement.ConflictResolution.skip,
				attributeStatement.getConflictResolution());
		assertEquals("foo", attributeStatement.getAssignedAttributeName());
		assertNotNull(attributeStatement.getFixedAttribute());
		assertEquals("val1", attributeStatement.getFixedAttribute().getValues().get(0).toString());
		
		GroupContents contentAB = groupsMan.getContents("/A/B", GroupContents.EVERYTHING);
		assertEquals(1, contentAB.getSubGroups().size());
		assertEquals("/A/B/D", contentAB.getSubGroups().get(0));

		GroupContents contentAC = groupsMan.getContents("/A/C", GroupContents.EVERYTHING);
		assertEquals(0, contentAC.getSubGroups().size());
		assertEquals(new I18nString("goo"), contentAC.getGroup().getDescription());
		
		groupsMan.removeGroup("/A/B/D", false);
		contentAB = groupsMan.getContents("/A/B", GroupContents.EVERYTHING);
		assertEquals(0, contentAB.getSubGroups().size());
		
		groupsMan.removeGroup("/A", true);
		contentRoot = groupsMan.getContents("/", GroupContents.EVERYTHING);
		assertEquals(0, contentRoot.getSubGroups().size());
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
		
		assertThat(rootChildren.size(), is(5));
		assertThat(rootChildren.contains("/"), is(true));
		assertThat(rootChildren.contains("/A"), is(true));
		assertThat(rootChildren.contains("/A/B"), is(true));
		assertThat(rootChildren.contains("/A/C"), is(true));
		assertThat(rootChildren.contains("/A/B/D"), is(true));

		Set<String> abChildren = groupsMan.getChildGroups("/A/B");
		
		assertThat(abChildren.toString(), abChildren.size(), is(2));
		assertThat(abChildren.contains("/A/B"), is(true));
		assertThat(abChildren.contains("/A/B/D"), is(true));
	}
	
	@Test 
	public void isPresentGroup() throws Exception
	{
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		assertThat(groupsMan.isPresent("/A"), is(true));
		assertThat(groupsMan.isPresent("/B"), is(false));
	}
	
	@Test
	public void shouldReturnAllGroupsByWildcard() throws EngineException
	{
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		groupsMan.addGroup(ab);

		List<Group> groups = groupsMan.getGroupsByWildcard("/**");
		
		assertThat(groups, hasItems(a, ab, new Group("/")));
		assertThat(groups.size(), is(3));
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
		
		assertThat(groups, hasItems(a, ab));
		assertThat(groups.size(), is(2));
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

	protected void assertExceptionType(Throwable exception, Class<?> type)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(type);
	}
}
