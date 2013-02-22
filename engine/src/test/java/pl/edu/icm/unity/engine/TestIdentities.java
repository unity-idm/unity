/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.core.identity.PersistentIdentity;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.EntityParam;
import pl.edu.icm.unity.types.Group;
import pl.edu.icm.unity.types.GroupContents;
import pl.edu.icm.unity.types.Identity;
import pl.edu.icm.unity.types.IdentityParam;
import pl.edu.icm.unity.types.IdentityType;

public class TestIdentities extends DBIntegrationTestBase
{
	@Test
	public void testSyntaxes() throws Exception
	{
		List<IdentityType> idTypes = idsMan.getIdentityTypes();
		assertEquals(2, idTypes.size());
		assertEquals(PersistentIdentity.ID, idTypes.get(0).getIdentityTypeProvider().getId());
		assertEquals(X500Identity.ID, idTypes.get(1).getIdentityTypeProvider().getId());
	}

	@Test
	public void testCreate() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi", true, true);
		Identity id = idsMan.addIdentity(idParam, "");
		assertNotNull(id.getEntityId());
		assertEquals("CN=golbi", id.getValue());
		assertEquals(true, id.isEnabled());
		assertEquals(true, id.isLocal());
		
		IdentityParam idParam2 = new IdentityParam(X500Identity.ID, "CN=golbi2", true, false);
		Identity id2 = idsMan.addIdentity(idParam2, new EntityParam(id.getEntityId()));
		assertEquals("CN=golbi2", id2.getValue());
		assertEquals(id.getEntityId(), id2.getEntityId());
		assertEquals(true, id2.isEnabled());
		assertEquals(false, id2.isLocal());
		
		GroupContents contents = groupsMan.getContents("/", GroupContents.MEMBERS);
		assertEquals(1, contents.getMembers().size());
		assertEquals(id.getEntityId(), contents.getMembers().get(0));
		
		groupsMan.addGroup(new Group("/test"));
		groupsMan.addMemberFromParent("/test", new EntityParam(id.getEntityId()));
		contents = groupsMan.getContents("/test", GroupContents.MEMBERS);
		assertEquals(1, contents.getMembers().size());
		assertEquals(id.getEntityId(), contents.getMembers().get(0));
		
		
		groupsMan.addGroup(new Group("/test2"));
		groupsMan.addGroup(new Group("/test2/test"));
		try
		{
			groupsMan.addMemberFromParent("/test2/test", new EntityParam(id.getEntityId()));
			fail("Added to a group while is not in parent");
		} catch(IllegalGroupValueException e) {}
		

		try
		{
			groupsMan.removeMember("/", new EntityParam(id.getEntityId()));
			fail("removed member from /");
		} catch(IllegalGroupValueException e) {}

		try
		{
			groupsMan.removeMember("/test2", new EntityParam(id.getEntityId()));
			fail("removed non member");
		} catch(IllegalGroupValueException e) {}

		groupsMan.removeMember("/test", new EntityParam(id.getEntityId()));
		contents = groupsMan.getContents("/test", GroupContents.MEMBERS);
		assertEquals(0, contents.getMembers().size());
		
	}
}
