/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import static org.junit.Assert.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.types.Group;
import pl.edu.icm.unity.types.GroupContents;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml", "classpath:META-INF/test-components.xml"})
@ActiveProfiles("test")
public class TestGroups
{
	@Autowired
	private GroupsManagement groupsMan;
	
	@Before
	public void clear() throws IOException
	{
		FileUtils.deleteDirectory(new File("target/data"));
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
		Assert.assertEquals(1, contentRoot.getSubGroups().size());
		Assert.assertEquals("/A", contentRoot.getSubGroups().get(0).toString());
		Assert.assertEquals("foo", contentRoot.getSubGroups().get(0).getDescription());

		GroupContents contentA = groupsMan.getContents("/A", GroupContents.EVERYTHING);
		Assert.assertEquals(2, contentA.getSubGroups().size());
		Assert.assertEquals("/A/B", contentA.getSubGroups().get(0).toString());
		Assert.assertEquals("/A/C", contentA.getSubGroups().get(1).toString());
		Assert.assertEquals("goo", contentA.getSubGroups().get(1).getDescription());
		
		GroupContents contentAB = groupsMan.getContents("/A/B", GroupContents.EVERYTHING);
		Assert.assertEquals(1, contentAB.getSubGroups().size());
		Assert.assertEquals("/A/B/D", contentAB.getSubGroups().get(0).toString());
	}
}
