/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

public class PasswordVerificatorTest
{
	@Test
	public void shouldAssumeNotSetOnNullPassword() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);

		assertEquals(LocalCredentialState.notSet, verificator.checkCredentialState(null).getState());
	}

	@Test
	public void shouldDenyTooWeakPassword() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		credCfg.setMinScore(10);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		try
		{
			PasswordToken pt = new PasswordToken("TooSimple");
			verificator.prepareCredential(pt.toJson(), "", true);
			fail("Set too weak password");
		} catch (IllegalCredentialException e) {}
	}

	@Test
	public void shouldAcceptStrongPassword() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		credCfg.setMinScore(10);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		try
		{
			PasswordToken pt = new PasswordToken("horsesdontlikeraddish");
			verificator.prepareCredential(pt.toJson(), "", true);
		} catch (IllegalCredentialException e) 
		{
			fail("Didn't set good password");
		}
	}
	
	@Test
	public void shouldDenyTooShortPassword() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		credCfg.setMinLength(5);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		try
		{
			PasswordToken pt = new PasswordToken("q!0?");
			verificator.prepareCredential(pt.toJson(), "", true);
			fail("Set too short password");
		} catch (IllegalCredentialException e) {}
	}
	
	@Test
	public void shouldDenyPasswordWithSequence() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		credCfg.setDenySequences(true);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		try
		{
			PasswordToken pt = new PasswordToken("123q!#");
			verificator.prepareCredential(pt.toJson(), "", true);
			fail("Set password wth sequence");
		} catch (IllegalCredentialException e) {}
	}
	
	@Test
	public void shouldDenyPasswordTooFewClasses() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		credCfg.setMinClassesNum(2);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		try
		{
			PasswordToken pt = new PasswordToken("zqoqso");
			verificator.prepareCredential(pt.toJson(), "", true);
			fail("Set password with 1 class");
		} catch (IllegalCredentialException e) {}
	}
	
	@Test
	public void shouldDenyPasswordFromHistory1() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		credCfg.setHistorySize(1);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		PasswordToken pt = new PasswordToken("1asdz");
		String c1 = verificator.prepareCredential(pt.toJson(), "", true);
		try
		{
			verificator.prepareCredential(new PasswordToken("1asdz").toJson(), c1, true);
			fail("Set password which was used");
		} catch (IllegalCredentialException e) {}
	}

	@Test
	public void shouldDenyPasswordFromHistory2() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		credCfg.setHistorySize(2);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		PasswordToken pt = new PasswordToken("1asdz");
		String c1 = verificator.prepareCredential(pt.toJson(), "", true);
		pt = new PasswordToken("2asdz");
		String c2 = verificator.prepareCredential(pt.toJson(), c1, true);
		try
		{
			verificator.prepareCredential(new PasswordToken("1asdz").toJson(), c2, true);
			fail("Set password which was used");
		} catch (IllegalCredentialException e) {}
	}

	
	@Test
	public void shouldAcceptCurrentPasswordForNoHistory() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		credCfg.setHistorySize(0);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		PasswordToken pt = new PasswordToken("1asdz");
		String c1 = verificator.prepareCredential(pt.toJson(), "", true);
		try
		{
			verificator.prepareCredential(new PasswordToken("1asdz").toJson(), c1, true);
		} catch (IllegalCredentialException e) 
		{
			fail("Coudn't set password which was used");
		}
	}
	
	
	
	@Test
	public void shouldAcceptUsedPasswordRemovedFromHistory1() throws Exception
	{
		PasswordVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		credCfg.setHistorySize(1);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		PasswordToken pt = new PasswordToken("1asdz");
		String c1 = verificator.prepareCredential(pt.toJson(), "", true);
		pt = new PasswordToken("2asdz");
		String c2 = verificator.prepareCredential(pt.toJson(), c1, true);

		try
		{
			verificator.prepareCredential(new PasswordToken("1asdz").toJson(), c2, true);
		} catch (IllegalCredentialException e) 
		{
			fail("Coudn't set password which was used");
		}
	}

	@Test
	public void shouldAcceptUsedPasswordAfterDecreasingHistoryLength() throws Exception
	{
		PasswordVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		credCfg.setHistorySize(1);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		PasswordToken pt = new PasswordToken("1asdz");
		String c1 = verificator.prepareCredential(pt.toJson(), "", true);

		credCfg.setHistorySize(0);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		try
		{
			verificator.prepareCredential(new PasswordToken("1asdz").toJson(), c1, true);
		} catch (IllegalCredentialException e) 
		{
			fail("Coudn't set password which was used");
		}
	}
	
	@Test
	public void shouldReturnCorrectStateForValidPassword() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		String pass = verificator.prepareCredential(new PasswordToken("1asdz").toJson(), "", true);
		
		assertEquals(LocalCredentialState.correct, verificator.checkCredentialState(pass).getState());
	}

	@Test
	public void shouldOutdatePassword() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		credCfg.setMaxAge(100);
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		String pass = verificator.prepareCredential(new PasswordToken("1asdz").toJson(), "", true);
		Thread.sleep(110);
		
		assertTrue(verificator.isSupportingInvalidation());
		assertEquals(LocalCredentialState.outdated, verificator.checkCredentialState(pass).getState());
	}

	@Test
	public void shouldReturnOutdatedForInvalidatedPassword() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		String c6 = verificator.prepareCredential(new PasswordToken("1qaZ2wsX").toJson(), "", true);
		
		String c7 = verificator.invalidate(c6);
		assertEquals(LocalCredentialState.outdated, verificator.checkCredentialState(c7).getState());
	}
	
	@Test
	public void shouldResetStateAfterSettingNewPassOnInvalidated() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		String c6 = verificator.prepareCredential(new PasswordToken("1qaZ2wsX").toJson(), "", true);
		String c7 = verificator.invalidate(c6);
		String c8 = verificator.prepareCredential(new PasswordToken("1qaZ2wsX2").toJson(), c7, true);

		assertEquals(LocalCredentialState.correct, verificator.checkCredentialState(c8).getState());
	}

	@Test
	public void shouldReturnOutdatedStateWhenScryptParamsAreChangedWithoutPassword() throws Exception
	{
		LocalCredentialVerificator verificator = new PasswordVerificator(null, null);
		PasswordCredential credCfg = getEmpty();
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		String withOldHash = verificator.prepareCredential(new PasswordToken("1qaZ2wsX").toJson(), "", true);
		
		credCfg.setScryptParams(new ScryptParams(11));
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		CredentialPublicInformation credInfo = verificator.checkCredentialState(withOldHash);
		assertEquals(LocalCredentialState.outdated, credInfo.getState());
	}

	@Test
	public void shouldRehashAndReturnCorrectWhenScryptParamsAreChangedAndPasswordGiven() throws Exception
	{
		CredentialHelper credHelper = mock(CredentialHelper.class);
		PasswordVerificator verificator = new PasswordVerificator(null, credHelper);
		IdentityResolver identityResolver = mock(IdentityResolver.class);
		verificator.setIdentityResolver(identityResolver);
		EntityWithCredential entityWithCred = new EntityWithCredential();
		when(identityResolver.resolveIdentity(eq("username"), any(), any())).thenReturn(entityWithCred);
		
		PasswordCredential credCfg = getEmpty();
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		String withOldHash = verificator.prepareCredential(new PasswordToken("1qaZ2wsX").toJson(), "", true);
		entityWithCred.setCredentialValue(withOldHash);
		
		credCfg.setScryptParams(new ScryptParams(11));
		verificator.setSerializedConfiguration(JsonUtil.serialize(credCfg.getSerializedConfiguration()));
		
		AuthenticationResult result = verificator.checkPassword("username", "1qaZ2wsX", null);
		assertEquals(Status.success, result.getStatus());
		verify(credHelper).updateCredential(eq(0L), eq(null), anyString());
	}

	
	private PasswordCredential getEmpty()
	{
		PasswordCredential credCfg = new PasswordCredential();
		credCfg.setMinLength(1);
		credCfg.setMinScore(0);
		credCfg.setDenySequences(false);
		credCfg.setHistorySize(0);
		credCfg.setMaxAge(PasswordCredential.MAX_AGE_UNDEF);
		credCfg.setMinClassesNum(1);
		credCfg.setScryptParams(new ScryptParams(10));
		return credCfg;
	}
}
