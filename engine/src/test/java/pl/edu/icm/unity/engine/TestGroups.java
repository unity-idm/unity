/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;
import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.types.Group;
import pl.edu.icm.unity.types.GroupContents;
import pl.edu.icm.unity.types.IdentityType;


public class TestGroups extends DBIntegrationTestBase
{
	@Test
	public void testIdTypes() throws Exception
	{
		for (IdentityType idType: idsMan.getIdentityTypes())
			System.out.println(idType);
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
		} catch (IllegalGroupValueException e)
		{
			//OK
		}
		
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
		
		GroupContents contentRoot = groupsMan.getContents("/", GroupContents.EVERYTHING);
		assertEquals(1, contentRoot.getSubGroups().size());
		assertEquals("/A", contentRoot.getSubGroups().get(0).toString());
		assertEquals("foo", contentRoot.getSubGroups().get(0).getDescription());

		GroupContents contentA = groupsMan.getContents("/A", GroupContents.EVERYTHING);
		assertEquals(2, contentA.getSubGroups().size());
		assertEquals("/A/B", contentA.getSubGroups().get(0).toString());
		assertEquals("/A/C", contentA.getSubGroups().get(1).toString());
		assertEquals("goo", contentA.getSubGroups().get(1).getDescription());
		
		GroupContents contentAB = groupsMan.getContents("/A/B", GroupContents.EVERYTHING);
		assertEquals(1, contentAB.getSubGroups().size());
		assertEquals("/A/B/D", contentAB.getSubGroups().get(0).toString());
		
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
