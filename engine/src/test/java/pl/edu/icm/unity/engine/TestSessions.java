/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;


public class TestSessions extends DBIntegrationTestBase
{
	@Autowired
	protected SessionManagement sessionMan;
	
	@Test
	public void test() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1", true);
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);

		LoginSession s = new LoginSession(null, new Date(), null, id.getEntityId(), "test"); 
		String sesId = sessionMan.createSession(s);
		s.setId(sesId);
		LoginSession ret = sessionMan.getSession(sesId);
		testEquals(s, ret);
		
		sessionMan.updateSessionAttributes(sesId, "a1", "a1Val");
		sessionMan.updateSessionAttributes(sesId, "a2", "a2Val");
		
		s.getSessionData().put("a1", "a1Val");
		s.getSessionData().put("a2", "a2Val");
		ret = sessionMan.getSession(sesId);
		testEquals(s, ret);
		
		long expire = System.currentTimeMillis() + 5000;
		sessionMan.updateSessionExpirtaion(sesId, new Date(expire));
		ret = sessionMan.getSession(sesId);
		s.setExpires(new Date(expire));
		testEquals(s, ret);
		
		sessionMan.removeSession(sesId);
		
		try
		{
			sessionMan.getSession(sesId);
			fail("Session was not removed");
		} catch (WrongArgumentException e)
		{
			//OK
		}
	}
	
	private void testEquals(LoginSession s, LoginSession ret)
	{
		assertEquals(s.getId(), ret.getId());
		assertEquals(s.getEntityId(), ret.getEntityId());
		assertEquals(s.getExpires(), ret.getExpires());
		assertEquals(s.getRealm(), ret.getRealm());
		assertEquals(s.getStarted(), ret.getStarted());
		assertEquals(s.getSessionData(), ret.getSessionData());
	}
	
}
