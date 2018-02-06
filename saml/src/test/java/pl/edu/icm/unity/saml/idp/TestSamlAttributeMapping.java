/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlBase64Binary;
import org.junit.Test;

import pl.edu.icm.unity.saml.idp.DefaultSamlAttributesMapper;
import pl.edu.icm.unity.saml.idp.SamlAttributeMapper;
import pl.edu.icm.unity.stdext.attr.IntegerAttribute;
import pl.edu.icm.unity.stdext.attr.JpegImageAttribute;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;

public class TestSamlAttributeMapping
{
	@Test
	public void stringAttributeIsMapped()
	{
		SamlAttributeMapper mapper = new DefaultSamlAttributesMapper();
		
		Attribute unityA = StringAttribute.of("attr1", "/", "val1");
		AttributeType samlA = mapper.convertToSaml(unityA);
		assertEquals("attr1", samlA.getName());
		assertEquals(1, samlA.sizeOfAttributeValueArray());
		assertEquals("val1", ((XmlAnySimpleType)samlA.getAttributeValueArray(0)).getStringValue());
	}

	@Test
	public void integerAttributeIsHandled()
	{
		SamlAttributeMapper mapper = new DefaultSamlAttributesMapper();
		List<Long> vals = new ArrayList<Long>();
		vals.add(1234l);
		vals.add(1l);
		Attribute unityA = IntegerAttribute.of("attr1", "/", vals);
		AttributeType samlA = mapper.convertToSaml(unityA);
		assertEquals("attr1", samlA.getName());
		assertEquals(2, samlA.sizeOfAttributeValueArray());
		assertEquals("1234", ((XmlAnySimpleType)samlA.getAttributeValueArray(0)).getStringValue());
		assertEquals("1", ((XmlAnySimpleType)samlA.getAttributeValueArray(1)).getStringValue());
	}
	
	@Test
	public void imageAttributeIsHandled()
	{
		SamlAttributeMapper mapper = new DefaultSamlAttributesMapper();
		BufferedImage bi = new BufferedImage(10, 20, BufferedImage.TYPE_INT_ARGB);
		Attribute unityA = JpegImageAttribute.of("attr1", "/", bi);
		AttributeType samlA = mapper.convertToSaml(unityA);
		assertEquals("attr1", samlA.getName());
		assertEquals(1, samlA.sizeOfAttributeValueArray());
		byte[] fromSaml = ((XmlBase64Binary)samlA.getAttributeValueArray(0)).getByteArrayValue();
		byte[] orig = new JpegImageAttributeSyntax().serialize(bi);
		assertTrue(Arrays.equals(orig, fromSaml));
	}
	
	@Test
	public void emailAttributeIsHandled()
	{
		SamlAttributeMapper mapper = new DefaultSamlAttributesMapper();
		VerifiableEmail email = new VerifiableEmail("add@example.com");
		Attribute unityA = VerifiableEmailAttribute.of("attr1", "/", email);
		AttributeType samlA = mapper.convertToSaml(unityA);
		assertEquals("attr1", samlA.getName());
		assertEquals(1, samlA.sizeOfAttributeValueArray());
		assertEquals("add@example.com", ((XmlAnySimpleType)samlA.getAttributeValueArray(0)).getStringValue());
	}
}
