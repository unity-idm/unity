/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlBase64Binary;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlDouble;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlLong;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.w3c.dom.Element;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;

/**
 * Default mapper of attributes to/from SAML.
 * 
 * @author K. Benedyczak
 */
public class DefaultSamlAttributesMapper implements SamlAttributeMapper
{
	private static final Map<String, ValueToSamlConverter> VALUE_TO_SAML;
	
	static {
		ValueToSamlConverter[] converters = new ValueToSamlConverter[] {
				new StringValueToSamlConverter(),
				new EmailValueToSamlConverter(),
				new IntegerValueToSamlConverter(),
				new FloatingValueToSamlConverter(),
				new ImageValueToSamlConverter()
		};
		Map<String, ValueToSamlConverter> map = new HashMap<>();
		for (ValueToSamlConverter conv: converters)
		{
			for (String syntax: conv.getSupportedSyntaxes())
				map.put(syntax, conv);
		}
		VALUE_TO_SAML = Map.copyOf(map);
	}
	
	@Override
	public boolean isHandled(Attribute unityAttribute)
	{
		String syntax = unityAttribute.getValueSyntax();
		return VALUE_TO_SAML.containsKey(syntax);
	}

	@Override
	public AttributeType convertToSaml(Attribute unityAttribute)
	{
		AttributeType ret = AttributeType.Factory.newInstance();
		ret.setName(unityAttribute.getName());
		String syntax = unityAttribute.getValueSyntax();
		ValueToSamlConverter converter = VALUE_TO_SAML.get(syntax);
		if (converter == null)
		{
			throw new IllegalStateException("There is no attribute type converter for " + syntax);
		}
		List<String> unityValues = unityAttribute.getValues();
		for (String unityValue : unityValues)
		{
			XmlObject converted = converter.convertValueToSaml(unityValue);
			XmlObject attributeValue = ret.addNewAttributeValue();
			attributeValue.set(converted);
			setSchemaTypeAttribute(attributeValue, converted.schemaType());
		}
		return ret;
	}

	private static void setSchemaTypeAttribute(XmlObject value, SchemaType schemaType)
	{
		QName schemaTypeName = schemaType.getName();
		if (schemaTypeName == null || !W3C_XML_SCHEMA_NS_URI.equals(schemaTypeName.getNamespaceURI()))
			throw new IllegalArgumentException("SAML attribute value must use an XML Schema type");
		Element element = (Element) value.getDomNode();
		element.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsi", W3C_XML_SCHEMA_INSTANCE_NS_URI);
		element.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:xs", W3C_XML_SCHEMA_NS_URI);
		element.setAttributeNS(W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:type", "xs:" + schemaTypeName.getLocalPart());
	}

	@Override
	public <T extends XmlObject> T convertFromSaml(AttributeType attribute, int valueIndex, Class<T> valueClass,
			SchemaType valueType)
	{
		try
		{
			XmlObject converted = XmlBeans.getContextTypeLoader().parse(
					attribute.getAttributeValueArray(valueIndex).newInputStream(),
					valueType,
					null);
			return valueClass.cast(converted);
		} catch (XmlException | IOException e)
		{
			throw new IllegalArgumentException("Can not parse SAML attribute value", e);
		}
	}

	private interface ValueToSamlConverter
	{
		XmlObject convertValueToSaml(String value);
		String[] getSupportedSyntaxes();
	}
	
	private static class StringValueToSamlConverter implements ValueToSamlConverter
	{
		@Override
		public XmlObject convertValueToSaml(String value)
		{
			XmlString v = XmlString.Factory.newInstance();
			v.setStringValue(value);
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
		private final VerifiableEmailAttributeSyntax syntax = new VerifiableEmailAttributeSyntax();
		
		@Override
		public XmlObject convertValueToSaml(String value)
		{
			XmlString v = XmlString.Factory.newInstance();
			VerifiableEmail email = syntax.convertFromString(value);
			v.setStringValue(email.getValue());
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
		private final IntegerAttributeSyntax syntax = new IntegerAttributeSyntax();
		
		@Override
		public XmlObject convertValueToSaml(String value)
		{
			XmlLong v = XmlLong.Factory.newInstance();
			v.setLongValue(syntax.convertFromString(value));
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
		private final FloatingPointAttributeSyntax syntax = new FloatingPointAttributeSyntax();
		
		@Override
		public XmlObject convertValueToSaml(String value)
		{
			XmlDouble v = XmlDouble.Factory.newInstance();
			v.setDoubleValue(syntax.convertFromString(value));
			return v;
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {FloatingPointAttributeSyntax.ID};
		}
	}

	private static class ImageValueToSamlConverter implements ValueToSamlConverter
	{
		private static final ImageAttributeSyntax syntax = new ImageAttributeSyntax();
		
		@Override
		public XmlObject convertValueToSaml(String value)
		{
			UnityImage decoded = syntax.convertFromString(value);
			byte[] octets = decoded.getImage();
			//that's a trick... Factory is both an inherited static variable and a nested class 
			//(and both have newInstance() method)- this ensures we call the other.
			XmlBase64Binary v = ((XmlBase64Binary.Factory)null).newInstance();
			v.setByteArrayValue(octets);
			return v;
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {ImageAttributeSyntax.ID};
		}
	}
}
