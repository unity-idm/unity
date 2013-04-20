/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.ldaputils.LDAPAttributeType;
import pl.edu.icm.unity.ldaputils.LDAPAttributeTypesLoader;

public class TestLdifParser
{
	@Test
	public void test() throws Exception
	{
		Reader r = new FileReader("src/main/resources/pl/edu/icm/unity/server/core/ldapschema/core.schema");
		List<LDAPAttributeType> loaded = LDAPAttributeTypesLoader.loadWithInheritance(r, null);
		
		Set<String> syntax = new HashSet<String>();
		for (LDAPAttributeType at: loaded)
		{
			Assert.assertNotNull(at.getOid());
			Assert.assertNotNull(at.getNames());
			String s = at.getSyntax();
			if (s == null)
				Assert.fail("No syntax: " + at);
			if (s != null)
			{
				if (s.contains("{"))
					s = s.substring(0, s.indexOf("{"));
				syntax.add(s);
			}

		}
		List<String> syntaxL = new ArrayList<String>();
		syntaxL.addAll(syntax);
		Collections.sort(syntaxL);
		for (String s: syntaxL)
			System.out.println(s);
	}
}
