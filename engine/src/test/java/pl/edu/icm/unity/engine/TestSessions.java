/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.SessionManagement.AttributeUpdater;
import pl.edu.icm.unity.server.utils.TimeUtil;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
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
		
		checkLastAuthnAttribute(s.getEntityId());
		
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
	
	private void checkLastAuthnAttribute(long entityId) throws EngineException
	{
		Collection<AttributeExt<?>> attrs = attrsMan.getAllAttributes(new EntityParam(entityId), false, "/", 
				SystemAttributeTypes.LAST_AUTHENTICATION, false);
		assertThat(attrs.size(), is(1));
		AttributeExt<?> lastAuthn = attrs.iterator().next();
		assertThat(lastAuthn.getValues().size(), is(1));
		String date = (String) lastAuthn.getValues().get(0); 
		LocalDateTime parsed = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		assertThat(parsed.isBefore(LocalDateTime.now()), is(true));
		assertThat(parsed.isBefore(LocalDateTime.now().minus(1, ChronoUnit.MINUTES)), is(false));
	}
	
	private void testEquals(LoginSession s, LoginSession ret)
	{
		assertEquals(s.getId(), ret.getId());
		assertEquals(s.getEntityId(), ret.getEntityId());
		assertEquals(TimeUtil.roundToS(s.getExpires()), TimeUtil.roundToS(ret.getExpires()));
		assertEquals(s.getMaxInactivity(), ret.getMaxInactivity());
		assertEquals(s.getRealm(), ret.getRealm());
		assertEquals(TimeUtil.roundToS(s.getStarted()), TimeUtil.roundToS(ret.getStarted()));
		assertEquals(s.getSessionData(), ret.getSessionData());
	}
	
}
