/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import static org.junit.Assert.*;
import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemoteInformationBase;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;

public class TestMapActions
{
	@Test
	public void testMapAttribute() throws EngineException
	{
		MapAttributeAction mapAction = new MapAttributeAction(new String[] {"attr(.+)", "attrUNITY", "/A/B"});
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addAttribute(new RemoteAttribute("attribute", "v1"));
		input.addAttribute(new RemoteAttribute("other", "v1"));
		
		mapAction.invoke(input);
		
		assertNotNull(input.getAttributes().get("attrUNITY"));
		assertNotNull(input.getAttributes().get("other"));
		assertEquals("attrUNITY", input.getAttributes().get("attrUNITY").getMetadata().get(
				RemoteInformationBase.UNITY_ATTRIBUTE));
		assertEquals("/A/B", input.getAttributes().get("attrUNITY").getMetadata().get(
				RemoteInformationBase.UNITY_GROUP));
	}

	@Test
	public void testMapGroup() throws EngineException
	{
		MapGroupAction mapAction = new MapGroupAction(new String[] {"/grou(.+)", "/groupUNITY"});
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addGroup(new RemoteGroupMembership("/group"));
		input.addGroup(new RemoteGroupMembership("/other"));
		
		mapAction.invoke(input);
		
		assertNotNull(input.getGroups().get("/groupUNITY"));
		assertNotNull(input.getGroups().get("/other"));
		assertEquals("/groupUNITY", input.getGroups().get("/groupUNITY").getMetadata().get(
				RemoteInformationBase.UNITY_GROUP));
	}
	
	@Test
	public void testMapIdentity() throws EngineException
	{
		MapIdentityAction mapAction = new MapIdentityAction(new String[] {"id(.+)", "idUNITY", "cr"});
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("idfoo", "tt"));
		input.addIdentity(new RemoteIdentity("other", "tt"));
		
		mapAction.invoke(input);
		
		assertNotNull(input.getIdentities().get("idUNITY"));
		assertNotNull(input.getIdentities().get("other"));
		assertEquals("idUNITY", input.getIdentities().get("idUNITY").getMetadata().get(
				RemoteInformationBase.UNITY_IDENTITY));
		assertEquals("cr", input.getIdentities().get("idUNITY").getMetadata().get(
				RemoteInformationBase.UNITY_IDENTITY_CREDREQ));
	}
}
