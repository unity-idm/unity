/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Attribute;

public class TestAttributesExtraction
{
	@Test
	public void testX500Extraction()
	{
		X500Identity id = new X500Identity();
		Map<String, String> spec = new HashMap<String, String>();
		spec.put("cn", "cn1");
		spec.put("o", "o1");
		spec.put("l", "l1");
		List<Attribute<?>> extracted = id.extractAttributes("CN=user,OU=org unit,O=org,C=PL", spec);
		Assert.assertEquals(2, extracted.size());
		Assert.assertEquals("cn1", extracted.get(0).getName());
		Assert.assertEquals("user", extracted.get(0).getValues().get(0));

		Assert.assertEquals("o1", extracted.get(1).getName());
		Assert.assertEquals("org", extracted.get(1).getValues().get(0));

	}

	@Test
	public void testUsernameExtraction()
	{
		UsernameIdentity id = new UsernameIdentity();
		Map<String, String> spec = new HashMap<String, String>();
		spec.put("uid", "username");
		List<Attribute<?>> extracted = id.extractAttributes("userX", spec);
		Assert.assertEquals(1, extracted.size());
		Assert.assertEquals("username", extracted.get(0).getName());
		Assert.assertEquals("userX", extracted.get(0).getValues().get(0));
	}
}
