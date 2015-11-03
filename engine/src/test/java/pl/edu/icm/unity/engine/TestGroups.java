/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import org.junit.Test;

import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeStatement2;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
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
		for (IdentityType idType: idsMan.getIdentityTypes())
			System.out.println(idType);
	}
	
	@Test
	public void testGetContentsWithLimitedAuthz() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUser(AuthorizationManagerImpl.USER_ROLE);
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		ab.setDescription(new I18nString("d-n"));
		groupsMan.addGroup(ab);
		Group ac = new Group("/A/C");
		groupsMan.addGroup(ac);
		
		EntityParam ep = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user1"));
		groupsMan.addMemberFromParent("/A", ep);
		groupsMan.addMemberFromParent("/A/B", ep);
		
		setupUserContext("user1", false);
		
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

		try
		{
			groupsMan.getContents("/A/C", GroupContents.EVERYTHING);
			fail("Should get authZ error for group where is not a member");
		} catch (AuthorizationException e)
		{
			//OK
		}
	}

	@Test
	public void testCreate() throws Exception
	{
		StringBuilder sb = new StringBuilder("/");
		for (int i=0; i<201; i++)
			sb.append("a");
		Group tooBig = new Group(sb.toString());
		try
		{
			groupsMan.addGroup(tooBig);
			fail("Managed to add a too big group");
		} catch (WrongArgumentException e)
		{
			//OK
		}
		
		AttributeType atFoo = new AttributeType("foo", new StringAttributeSyntax());
		attrsMan.addAttributeType(atFoo);
		
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

		AttributeStatement2[] statements = new AttributeStatement2[2];
		statements[0] = AttributeStatement2.getFixedEverybodyStatement(
				new StringAttribute("foo", "/A", AttributeVisibility.full, "val1"));
		statements[1] = AttributeStatement2.getFixedStatement(
				new StringAttribute("foo", "/A", AttributeVisibility.full, "val1"),
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
		AttributeStatement2 attributeStatement = contentA.getGroup().getAttributeStatements()[0];
		assertEquals(AttributeStatement2.ConflictResolution.skip,
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
		
		try
		{
			groupsMan.removeGroup("/A", false);
			fail("Removed non empty group, with recursive == false");
		} catch (IllegalGroupValueException e)
		{
			//OK
		}
		try
		{
			groupsMan.removeGroup("/", true);
			fail("Removed root group");
		} catch (IllegalGroupValueException e)
		{
			//OK
		}
		
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
}
