/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.SessionManagement.AttributeUpdater;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;


public class TestSessions extends DBIntegrationTestBase
{
	@Autowired
	protected SessionManagement sessionMan;
	
	@Test
	public void test() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);

		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, -1, 100);
		AuthenticationRealm realm2 = new AuthenticationRealm("test2", "", 3, 33, -1, 100);
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", false, null);
		LoginSession ret = sessionMan.getSession(s.getId());
		testEquals(s, ret);
		LoginSession s2 = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", false, null);
		testEquals(s, s2);
		LoginSession s3 = sessionMan.getCreateSession(id.getEntityId(), realm2, "u1", false, null);
		assertNotEquals(s.getId(), s3.getId());
		
		sessionMan.updateSessionAttributes(s.getId(), new AttributeUpdater()
		{
			@Override
			public void updateAttributes(Map<String, String> sessionAttributes)
			{
				sessionAttributes.put("a1", "a1Val");
				sessionAttributes.put("a2", "a2Val");
			}
		});
				
		s.getSessionData().put("a1", "a1Val");
		s.getSessionData().put("a2", "a2Val");
		ret = sessionMan.getSession(s.getId());
		testEquals(s, ret);
		
		sessionMan.removeSession(s.getId(), false);
		
		try
		{
			sessionMan.getSession(s.getId());
			fail("Session was not removed");
		} catch (WrongArgumentException e)
		{
			//OK
		}
		
		sessionMan.removeSession(s3.getId(), false);
	}
	
	private void testEquals(LoginSession s, LoginSession ret)
	{
		assertEquals(s.getId(), ret.getId());
		assertEquals(s.getEntityId(), ret.getEntityId());
		assertEquals(s.getExpires(), ret.getExpires());
		assertEquals(s.getMaxInactivity(), ret.getMaxInactivity());
		assertEquals(s.getRealm(), ret.getRealm());
		assertEquals(s.getStarted(), ret.getStarted());
		assertEquals(s.getSessionData(), ret.getSessionData());
	}
	
}
