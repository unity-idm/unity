/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Test;

import eu.unicore.samly2.binding.HttpRedirectBindingSupport;


public class TestSAMLBindingUtils
{
	@Test
	public void testRedirectEncoding() throws UnsupportedEncodingException, IOException
	{
		String a = "someString~!@#$%^&*(\u0001\u0002\u0003\u0010\u2222";
		Assert.assertEquals(a, HttpRedirectBindingSupport.inflateSAMLRequest(
				HttpRedirectBindingSupport.toURLParam(a)));
	}
}
