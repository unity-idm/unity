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
				"\"maxAge\": 100" +
				"}");
		String serialized = verificator.getSerializedConfiguration();
		verificator.setSerializedConfiguration(serialized);
		
		assertEquals(LocalCredentialState.notSet, verificator.checkCredentialState(null));
		try
		{
			verificator.prepareCredential("q!0?", "");
			fail("Set too short password");
		} catch (IllegalCredentialException e) {}
		try
		{
			verificator.prepareCredential("123q!#", "");
			fail("Set password wth sequence");
		} catch (IllegalCredentialException e) {}
		try
		{
			verificator.prepareCredential("zqoqso", "");
			fail("Set password with 1 class");
		} catch (IllegalCredentialException e) {}
		
		String c1 = verificator.prepareCredential("1asdz", "");
		String c2 = verificator.prepareCredential("2asdz", c1);
		try
		{
			verificator.prepareCredential("1asdz", c2);
			fail("Set password which was used");
		} catch (IllegalCredentialException e) {}
		String c3 = verificator.prepareCredential("3asdz", c2);
		String c4 = verificator.prepareCredential("4asdz", c3);
		String c5 = verificator.prepareCredential("1asdz", c4);
		
		assertEquals(LocalCredentialState.correct, verificator.checkCredentialState(c5));
		Thread.sleep(100);
		assertEquals(LocalCredentialState.outdated, verificator.checkCredentialState(c5));
	}
}
