/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import org.junit.Test;

import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.attrstmnt.EverybodyStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.HasSubgroupAttributeStatement;


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
		a.setDescription("foo");
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		groupsMan.addGroup(ab);
		Group ac = new Group("/A/C");
		ac.setDescription("goo");
		groupsMan.addGroup(ac);
		
		EntityParam ep = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user1"));
		groupsMan.addMemberFromParent("/A", ep);
		groupsMan.addMemberFromParent("/A/B", ep);
		
		setupUserContext("user1", false);
		
		GroupContents rootC = groupsMan.getContents("/", GroupContents.EVERYTHING);
		assertEquals(1, rootC.getSubGroups().size());
		assertEquals("/A", rootC.getSubGroups().get(0));
		assertNull(rootC.getGroup());
		assertNull(rootC.getMembers());
		
		GroupContents aC = groupsMan.getContents("/A", GroupContents.EVERYTHING);
		assertEquals(1, aC.getSubGroups().size());
		assertEquals("/A/B", aC.getSubGroups().get(0));
		assertNull(aC.getGroup());
		assertNull(aC.getMembers());

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
		a.setDescription("foo");
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		groupsMan.addGroup(ab);
		Group ac = new Group("/A/C");
		ac.setDescription("goo");
		groupsMan.addGroup(ac);
		Group abd = new Group("/A/B/D");
		groupsMan.addGroup(abd);

		AttributeStatement[] statements = new AttributeStatement[2];
		statements[0] = new EverybodyStatement(
				new StringAttribute("foo", "/A", AttributeVisibility.full, "val1"), 
				AttributeStatement.ConflictResolution.skip);
		statements[1] = new HasSubgroupAttributeStatement(
				new StringAttribute("foo", "/A", AttributeVisibility.full, "val1"), 
				new StringAttribute("foo", "/A/B", AttributeVisibility.full, "ala"),
				AttributeStatement.ConflictResolution.skip);
		a.setAttributeStatements(statements);
		groupsMan.updateGroup("/A", a);
		
		Group root = new Group("should be ignored");
		root.setDescription("root desc");
		groupsMan.updateGroup("/", root);
		
		GroupContents contentRoot = groupsMan.getContents("/", GroupContents.EVERYTHING);
		assertEquals(1, contentRoot.getSubGroups().size());
		assertEquals("/A", contentRoot.getSubGroups().get(0));
		assertEquals("root desc", contentRoot.getGroup().getDescription());
		
		GroupContents contentA = groupsMan.getContents("/A", GroupContents.EVERYTHING);
		assertEquals("foo", contentA.getGroup().getDescription());
		assertEquals(2, contentA.getSubGroups().size());
		assertEquals("/A/B", contentA.getSubGroups().get(0));
		assertEquals("/A/C", contentA.getSubGroups().get(1));
		assertEquals(2, contentA.getGroup().getAttributeStatements().length);
		assertEquals(AttributeStatement.ConflictResolution.skip,
				contentA.getGroup().getAttributeStatements()[0].getConflictResolution());
		assertEquals("foo", contentA.getGroup().getAttributeStatements()[0].getAssignedAttribute().getName());
		assertEquals("val1", contentA.getGroup().getAttributeStatements()[0].getAssignedAttribute().
				getValues().get(0).toString());
		assertEquals(EverybodyStatement.NAME, contentA.getGroup().getAttributeStatements()[0].getName());
		
		GroupContents contentAB = groupsMan.getContents("/A/B", GroupContents.EVERYTHING);
		assertEquals(1, contentAB.getSubGroups().size());
		assertEquals("/A/B/D", contentAB.getSubGroups().get(0));

		GroupContents contentAC = groupsMan.getContents("/A/C", GroupContents.EVERYTHING);
		assertEquals(0, contentAC.getSubGroups().size());
		assertEquals("goo", contentAC.getGroup().getDescription());
		
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
}
