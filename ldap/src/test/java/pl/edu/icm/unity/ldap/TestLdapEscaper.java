/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pl.edu.icm.unity.ldap.client.LdapUnsafeArgsEscaper;

public class TestLdapEscaper
{
	@Test
	public void dnProperlyEscaped()
	{
		assertEquals("1234qwerty\\\\\\,\\+\\\"\\<\\>\\;", 
				LdapUnsafeArgsEscaper.escapeForUseAsDN("1234qwerty\\,+\"<>;"));
	}

	@Test
	public void filterProperlyEscaped()
	{
		assertEquals("1234qwerty\\5c\\2a\\28\\29\\00", 
				LdapUnsafeArgsEscaper.escapeLDAPSearchFilter("1234qwerty\\*()\u0000"));
	}
}
