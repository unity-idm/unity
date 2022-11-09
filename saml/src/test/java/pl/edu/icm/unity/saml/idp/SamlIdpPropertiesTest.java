/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import org.junit.Test;

public class SamlIdpPropertiesTest
{
	@Test
	public void shouldParseAllowedRequesters()
	{
//		Map<Integer, String> parsed = SamlIdpProperties.initAllowedRequesters(Lists.newArrayList("[1]http://url", "[11]http://url2"));
//
//		assertThat(parsed.size(), is(2));
//		assertThat(parsed.get(1), is("http://url"));
//		assertThat(parsed.get(11), is("http://url2"));
	}

	@Test
	public void shouldProtestOnInvalidSyntax()
	{
//		try
//		{
//			SamlIdpProperties.initAllowedRequesters(Lists.newArrayList("[]http://url2"));
//			fail("should fail to parse");
//		} catch (ConfigurationException e)
//		{
//			//ok
//		}
	}

	@Test
	public void shouldProtestOnInvalidSyntax2()
	{
//		try
//		{
//			SamlIdpProperties.initAllowedRequesters(Lists.newArrayList("[sd]http://url2"));
//			fail("should fail to parse");
//		} catch (ConfigurationException e)
//		{
//			//ok
//		}
		
	}

}
