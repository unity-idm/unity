/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.session.SessionManagement.AttributeUpdater;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.session.LastAuthenticationAttributeTypeProvider;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;


public class TestSessions extends DBIntegrationTestBase
{
	@Autowired
	protected SessionManagement sessionMan;

	@Test
	public void updatedSessionAttributesAreReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, -1, 100);
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null);
		
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
		
		LoginSession ret = sessionMan.getSession(s.getId());
		testEquals(s, ret);
	}

	
	@Test
	public void removedSessionIsNotReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, -1, 100);
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null);
		
		sessionMan.removeSession(s.getId(), false);
		
		catchException(sessionMan).getSession(s.getId());
		
		assertThat(caughtException(), isA(IllegalArgumentException.class));
	}

	@Test
	public void sessionsAreReamScoped() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);

		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, -1, 100);
		AuthenticationRealm realm2 = new AuthenticationRealm("test2", "", 3, 33, -1, 100);
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null);
		
		checkLastAuthnAttribute(s.getEntityId());
		
		LoginSession ret = sessionMan.getSession(s.getId());
		LoginSession s2 = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null);
		LoginSession s3 = sessionMan.getCreateSession(id.getEntityId(), realm2, "u1", null, null);

		testEquals(s, ret);
		testEquals(s, s2);
		assertNotEquals(s.getId(), s3.getId());
	}
	
	private void checkLastAuthnAttribute(long entityId) throws EngineException
	{
		Collection<AttributeExt> attrs = attrsMan.getAllAttributes(new EntityParam(entityId), false, "/", 
				LastAuthenticationAttributeTypeProvider.LAST_AUTHENTICATION, false);
		assertThat(attrs.size(), is(1));
		AttributeExt lastAuthn = attrs.iterator().next();
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
		assertEquals(s.getExpires(), ret.getExpires());
		assertEquals(s.getMaxInactivity(), ret.getMaxInactivity());
		assertEquals(s.getRealm(), ret.getRealm());
		assertEquals(s.getStarted(), ret.getStarted());
		assertEquals(s.getSessionData(), ret.getSessionData());
	}
	
}
