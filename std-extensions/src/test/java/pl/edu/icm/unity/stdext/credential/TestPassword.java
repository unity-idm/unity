/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import static org.junit.Assert.*;
import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

public class TestPassword
{
	@Test
	public void test() throws Exception
	{
		PasswordVerificatorFactory f = new PasswordVerificatorFactory();
		LocalCredentialVerificator verificator = f.newInstance();
		verificator.setSerializedConfiguration("{" +
				"\"minLength\": 5," +
				"\"historySize\": 3," +
				"\"minClassesNum\": 2," +
				"\"denySequences\": true," +
				"\"maxAge\": 100," +
				"\"max\": 100" +
				"}");
		String serialized = verificator.getSerializedConfiguration();
		verificator.setSerializedConfiguration(serialized);
		
		assertEquals(LocalCredentialState.notSet, verificator.checkCredentialState(null).getState());
		try
		{
			PasswordToken pt = new PasswordToken("q!0?");
			verificator.prepareCredential(pt.toJson(), "");
			fail("Set too short password");
		} catch (IllegalCredentialException e) {}
		try
		{
			PasswordToken pt = new PasswordToken("123q!#");
			verificator.prepareCredential(pt.toJson(), "");
			fail("Set password wth sequence");
		} catch (IllegalCredentialException e) {}
		try
		{
			PasswordToken pt = new PasswordToken("zqoqso");
			verificator.prepareCredential(pt.toJson(), "");
			fail("Set password with 1 class");
		} catch (IllegalCredentialException e) {}
		
		PasswordToken pt = new PasswordToken("1asdz");
		String c1 = verificator.prepareCredential(pt.toJson(), "");
		pt = new PasswordToken("2asdz");
		String c2 = verificator.prepareCredential(pt.toJson(), c1);
		try
		{
			verificator.prepareCredential(new PasswordToken("1asdz").toJson(), c2);
			fail("Set password which was used");
		} catch (IllegalCredentialException e) {}
		String c3 = verificator.prepareCredential(new PasswordToken("3asdz").toJson(), c2);
		String c4 = verificator.prepareCredential(new PasswordToken("4asdz").toJson(), c3);
		String c5 = verificator.prepareCredential(new PasswordToken("1asdz").toJson(), c4);
		
		assertEquals(LocalCredentialState.correct, verificator.checkCredentialState(c5).getState());
		Thread.sleep(500);
		assertEquals(LocalCredentialState.outdated, verificator.checkCredentialState(c5).getState());
		
		assertTrue(verificator.isSupportingInvalidation());
		String c6 = verificator.prepareCredential(new PasswordToken("1qaZ2wsX").toJson(), c5);
		assertEquals(LocalCredentialState.correct, verificator.checkCredentialState(c6).getState());
		String c7 = verificator.invalidate(c6);
		assertEquals(LocalCredentialState.outdated, verificator.checkCredentialState(c7).getState());
		String c8 = verificator.prepareCredential(new PasswordToken("1qaZ2wsX2").toJson(), c7);
		assertEquals(LocalCredentialState.correct, verificator.checkCredentialState(c8).getState());
	}
}
