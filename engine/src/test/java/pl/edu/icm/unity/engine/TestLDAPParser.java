/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.ldaputils.LDAPAttributeTypesConverter;
import pl.edu.icm.unity.types.basic.AttributeType;

import static org.junit.Assert.*;

/**
 * Tests loading of LDAP attribute types
 * @author K. Benedyczak
 */
public class TestLDAPParser extends SecuredDBIntegrationTestBase
{
	@Autowired 
	private LDAPAttributeTypesConverter converter; 
	
	@Test
	public void test() throws Exception
	{
		InputStream is = LDAPAttributeTypesConverter.class.getClassLoader().getResourceAsStream(
				"pl/edu/icm/unity/server/core/ldapschema/core.schema");
		
		Reader r = new InputStreamReader(is);
		List<AttributeType> attribtues = converter.convert(r);
		for (AttributeType at: attribtues)
			assertNotNull(at.getDescription());
		assertEquals(46, attribtues.size());
	}
}
