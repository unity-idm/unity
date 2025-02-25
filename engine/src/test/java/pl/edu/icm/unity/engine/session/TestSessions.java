/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static pl.edu.icm.unity.base.authn.AuthenticationOptionKey.authenticatorOnlyKey;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.session.SessionManagement.AttributeUpdater;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.session.SessionManagementImpl.SessionExpiredException;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;


public class TestSessions extends DBIntegrationTestBase
{
	@Autowired
	protected SessionManagement sessionMan;



	@Test
	public void createSessionAlwaysReturnNewSession() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, RememberMePolicy.disallow , 1, 100);
		LoginSession s1 = sessionMan.createSession(id.getEntityId(), realm, "u1", null, null, authenticatorOnlyKey("auth1"), null, null, null, null);
		LoginSession s2 = sessionMan.createSession(id.getEntityId(), realm, "u1", null, null, authenticatorOnlyKey("auth1"), null, null, null, null);	
		assertNotEquals(s1.getId(), s2.getId());	
	}
	
	
	
	@Test
	public void updatedSessionAttributesAreReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, RememberMePolicy.disallow , 1, 100);
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null, authenticatorOnlyKey("auth1"), null, null, null, null);
		
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
	public void additionalAuthenticationStateIsReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, RememberMePolicy.disallow , 1, 100);
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null, authenticatorOnlyKey("auth1"), null, null, null, null);
		
		sessionMan.recordAdditionalAuthentication(s.getId(), authenticatorOnlyKey("authnOption"));
				
		LoginSession ret = sessionMan.getSession(s.getId());
		assertThat(ret.getAdditionalAuthn().optionId).isEqualTo(authenticatorOnlyKey("authnOption"));
	}
	
	@Test
	public void authenticationMethodsAreReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, RememberMePolicy.disallow , 1, 100);
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null, authenticatorOnlyKey("auth1"), null, null, Set.of(AuthenticationMethod.pwd, AuthenticationMethod.sms), null);
		
				
		LoginSession ret = sessionMan.getSession(s.getId());
		assertThat(ret.getAuthenticationMethods()).containsExactlyInAnyOrder(AuthenticationMethod.sms, AuthenticationMethod.pwd);
	}
	
	@Test
	public void authenticationTimeIsReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, RememberMePolicy.disallow , 1, 100);
		Instant now = Instant.now();
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null, authenticatorOnlyKey("auth1"), null, null, null, now);
		LoginSession ret = sessionMan.getSession(s.getId());
		assertThat(ret.getAuthenticationTime().toEpochMilli()).isEqualTo(now.toEpochMilli());
	}

	@Test
	public void removedSessionIsNotReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, RememberMePolicy.disallow , 1, 100);
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null, authenticatorOnlyKey("auth1"), null, null, null, null);
		
		sessionMan.removeSession(s.getId(), false);
		
		Throwable error = catchThrowable(() -> sessionMan.getSession(s.getId()));
		
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void sessionsAreRealmScoped() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);

		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, RememberMePolicy.disallow , 1, 100);
		AuthenticationRealm realm2 = new AuthenticationRealm("test2", "", 3, 33, RememberMePolicy.disallow , 1, 100);
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null, 
				authenticatorOnlyKey("auth1"), null, null, null, null);
		
		checkLastAuthnAttributeWasRecentlySet(s.getEntityId());
		
		LoginSession ret = sessionMan.getSession(s.getId());
		LoginSession s2 = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null, 
				authenticatorOnlyKey("auth1"), null, null, null, null);
		LoginSession s3 = sessionMan.getCreateSession(id.getEntityId(), realm2, "u1", null, null, 
				authenticatorOnlyKey("auth1"), null, null, null, null);

		testEquals(s, ret);
		testEquals(s, s2);
		assertNotEquals(s.getId(), s3.getId());
	}

	@Test
	public void expiredSessionIsNotReturned() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, RememberMePolicy.disallow , 1, 1);
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null, authenticatorOnlyKey("auth1"), null, null, null, null);
		
		Thread.sleep(1001);
		
		Throwable error = catchThrowable(() -> sessionMan.getSession(s.getId()));

		assertThat(error).isInstanceOfAny(SessionExpiredException.class, IllegalArgumentException.class);
	}

	@Test
	public void expiredSessionIsNotProlonged() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");
		Identity id = idsMan.addEntity(toAdd, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);
		AuthenticationRealm realm = new AuthenticationRealm("test", "", 3, 33, RememberMePolicy.disallow , 1, 1);
		LoginSession s = sessionMan.getCreateSession(id.getEntityId(), realm, "u1", null, null, authenticatorOnlyKey("auth1"), null, null, null, null);
		
		Thread.sleep(1001);
		
		Throwable error = catchThrowable(() -> sessionMan.updateSessionActivity(s.getId()));
		
		assertThat(error).isInstanceOfAny(SessionExpiredException.class, IllegalArgumentException.class);
	}
	
	private void checkLastAuthnAttributeWasRecentlySet(long entityId) throws EngineException
	{
		Collection<AttributeExt> attrs = attrsMan.getAllAttributes(new EntityParam(entityId), false, "/", 
				LastAuthenticationAttributeTypeProvider.LAST_AUTHENTICATION, false);
		assertThat(attrs).hasSize(1);
		AttributeExt lastAuthn = attrs.iterator().next();
		assertThat(lastAuthn.getValues()).hasSize(1);
		String date = (String) lastAuthn.getValues().get(0); 
		LocalDateTime parsed = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		assertThat(parsed.isBefore(LocalDateTime.now())).isTrue();
		assertThat(parsed.isBefore(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))).isFalse();
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
