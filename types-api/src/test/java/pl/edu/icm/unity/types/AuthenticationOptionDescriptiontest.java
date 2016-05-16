/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;

import com.fasterxml.jackson.databind.JsonNode;

public class AuthenticationOptionDescriptiontest
{
	@Test
	public void legacyParsingWorks() throws IOException
	{
		JsonNode authnSetsNode = Constants.MAPPER.readTree(
				"\"[{\\\"authenticators\\\":[\\\"pwdWeb\\\", \\\"pwdWeb2\\\"]},"
				+ "{\\\"authenticators\\\":[\\\"certWeb\\\"]}]\"");
		List<AuthenticationOptionDescription> aSets = AuthenticationOptionDescription.
				parseLegacyAuthenticatorSets(authnSetsNode);
		assertEquals(2, aSets.size());
		assertEquals("pwdWeb", aSets.get(0).getPrimaryAuthenticator());
		assertEquals("pwdWeb2", aSets.get(0).getMandatory2ndAuthenticator());
		assertEquals("certWeb", aSets.get(1).getPrimaryAuthenticator());
		assertNull(aSets.get(1).getMandatory2ndAuthenticator());
	}
}
