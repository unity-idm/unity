/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlBase64Binary;
import org.apache.xmlbeans.XmlDouble;
import org.apache.xmlbeans.XmlLong;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;

import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;

/**
 * Default mapper of attributes to/from SAML.
 * Only string, enum integer and float attributes are supported.
 * 
 * 
 * @author K. Benedyczak
 */
public class DefaultSamlAttributesMapper implements SamlAttributeMapper
{
	private static final Map<String, ValueToSamlConverter> VALUE_TO_SAML = 
			new HashMap<String, DefaultSamlAttributesMapper.ValueToSamlConverter>();
	
	static {
		ValueToSamlConverter[] converters = new ValueToSamlConverter[] {
				new StringValueToSamlConverter(),
				new EmailValueToSamlConverter(),
				new IntegerValueToSamlConverter(),
				new FloatingValueToSamlConverter(),
				new JpegValueToSamlConverter()
		};

		for (ValueToSamlConverter conv: converters)
		{
			for (String syntax: conv.getSupportedSyntaxes())
				VALUE_TO_SAML.put(syntax, conv);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isHandled(Attribute<?> unityAttribute)
	{
		String syntax = unityAttribute.getAttributeSyntax().getValueSyntaxId();
		return VALUE_TO_SAML.containsKey(syntax);
	}

	@Override
	public AttributeType convertToSaml(Attribute<?> unityAttribute)
	{
		AttributeType ret = AttributeType.Factory.newInstance();
		ret.setName(unityAttribute.getName());
		String syntax = unityAttribute.getAttributeSyntax().getValueSyntaxId();
		ValueToSamlConverter converter = VALUE_TO_SAML.get(syntax);
		if (converter == null)
		{
			throw new IllegalStateException("There is no attribute type converter for " + syntax);
		}
		List<?> unityValues = unityAttribute.getValues();
		XmlObject[] xmlValues = new XmlObject[unityValues.size()];
		for (int i=0; i<xmlValues.length; i++)
			xmlValues[i] = converter.convertValueToSaml(unityValues.get(i));
		ret.setAttributeValueArray(xmlValues);
		return ret;
	}


	private interface ValueToSamlConverter
	{
		XmlObject convertValueToSaml(Object value);
		String[] getSupportedSyntaxes();
	}
	
	private static class StringValueToSamlConverter implements ValueToSamlConverter
	{
		@Override
		public XmlObject convertValueToSaml(Object value)
		{
			XmlString v = XmlString.Factory.newInstance();
			v.setStringValue((String) value);
			return v;
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {StringAttributeSyntax.ID, EnumAttributeSyntax.ID};
		}
	}

	private static class EmailValueToSamlConverter implements ValueToSamlConverter
	{
		@Override
		public XmlObject convertValueToSaml(Object value)
		{
			XmlString v = XmlString.Factory.newInstance();
			v.setStringValue(((VerifiableEmail) value).getValue());
			return v;
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {VerifiableEmailAttributeSyntax.ID};
		}
	}

	private static class IntegerValueToSamlConverter implements ValueToSamlConverter
	{
		@Override
		public XmlObject convertValueToSaml(Object value)
		{
			XmlLong v = XmlLong.Factory.newInstance();
			v.setLongValue((Long) value);
			return v;
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {IntegerAttributeSyntax.ID};
		}
	}

	private static class FloatingValueToSamlConverter implements ValueToSamlConverter
	{
		@Override
		public XmlObject convertValueToSaml(Object value)
		{
			XmlDouble v = XmlDouble.Factory.newInstance();
			v.setDoubleValue((Double)value);
			return v;
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {FloatingPointAttributeSyntax.ID};
		}
	}

	private static class JpegValueToSamlConverter implements ValueToSamlConverter
	{
		@Override
		public XmlObject convertValueToSaml(Object value)
		{
			byte[] octets = new JpegImageAttributeSyntax().serialize((BufferedImage) value);
			XmlBase64Binary v = XmlBase64Binary.Factory.newInstance();
			v.setByteArrayValue(octets);
			return v;
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {JpegImageAttributeSyntax.ID};
		}
	}
}
