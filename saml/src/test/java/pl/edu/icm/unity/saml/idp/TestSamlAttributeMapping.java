/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;



import static javax.xml.XMLConstants.NULL_NS_URI;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlBase64Binary;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlDouble;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlLong;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.image.ImageType;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.attr.ImageAttribute;
import pl.edu.icm.unity.stdext.attr.IntegerAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import xmlbeans.org.oasis.saml2.assertion.AttributeDocument;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;

public class TestSamlAttributeMapping
{
	@Test
	public void shouldMapStringAttribute()
	{
		SamlAttributeMapper mapper = new DefaultSamlAttributesMapper();
		
		Attribute unityA = StringAttribute.of("attr1", "/", "val1");
		AttributeType samlA = mapper.convertToSaml(unityA);
		RoundTrippedValue value = roundTrip(samlA);

		assertThat(samlA.getName()).isEqualTo("attr1");
		assertThat(samlA.sizeOfAttributeValueArray()).isEqualTo(1);
		assertThat(value.schemaType()).isEqualTo(XmlString.type.getName());
		assertThat(value.text()).isEqualTo("val1");
	}

	@Test
	public void shouldMapIntegerAttribute()
	{
		SamlAttributeMapper mapper = new DefaultSamlAttributesMapper();
		List<Long> vals = List.of(1234L, 1L);
		Attribute unityA = IntegerAttribute.of("attr1", "/", vals);

		AttributeType samlA = mapper.convertToSaml(unityA);
		List<RoundTrippedValue> values = roundTripAllValues(samlA);

		assertThat(samlA.getName()).isEqualTo("attr1");
		assertThat(samlA.sizeOfAttributeValueArray()).isEqualTo(2);
		assertThat(values).extracting(RoundTrippedValue::schemaType).containsOnly(XmlLong.type.getName());
		assertThat(values).extracting(value -> Long.parseLong(value.text())).containsExactly(1234L, 1L);
	}

	@Test
	public void shouldMapFloatingPointAttribute()
	{
		SamlAttributeMapper mapper = new DefaultSamlAttributesMapper();
		Attribute unityA = FloatingPointAttribute.of("attr1", "/", 123.25);

		AttributeType samlA = mapper.convertToSaml(unityA);
		RoundTrippedValue value = roundTrip(samlA);

		assertThat(samlA.getName()).isEqualTo("attr1");
		assertThat(samlA.sizeOfAttributeValueArray()).isEqualTo(1);
		assertThat(value.schemaType()).isEqualTo(XmlDouble.type.getName());
		assertThat(Double.parseDouble(value.text())).isEqualTo(123.25);
	}
	
	@Test
	public void shouldMapImageAttribute()
	{
		SamlAttributeMapper mapper = new DefaultSamlAttributesMapper();
		BufferedImage bi = new BufferedImage(10, 20, BufferedImage.TYPE_INT_ARGB);
		UnityImage image = new UnityImage(bi, ImageType.JPG);
		Attribute unityA = ImageAttribute.of("attr1", "/", image);

		AttributeType samlA = mapper.convertToSaml(unityA);
		RoundTrippedValue value = roundTrip(samlA);

		assertThat(samlA.getName()).isEqualTo("attr1");
		assertThat(samlA.sizeOfAttributeValueArray()).isEqualTo(1);
		assertThat(value.schemaType()).isEqualTo(XmlBase64Binary.type.getName());
		assertThat(Base64.getMimeDecoder().decode(value.text())).isEqualTo(image.getImage());
	}
	
	@Test
	public void shouldMapEmailAttribute()
	{
		SamlAttributeMapper mapper = new DefaultSamlAttributesMapper();
		VerifiableEmail email = new VerifiableEmail("add@example.com");
		Attribute unityA = VerifiableEmailAttribute.of("attr1", "/", email);

		AttributeType samlA = mapper.convertToSaml(unityA);
		RoundTrippedValue value = roundTrip(samlA);

		assertThat(samlA.getName()).isEqualTo("attr1");
		assertThat(samlA.sizeOfAttributeValueArray()).isEqualTo(1);
		assertThat(value.schemaType()).isEqualTo(XmlString.type.getName());
		assertThat(value.text()).isEqualTo("add@example.com");
	}

	private RoundTrippedValue roundTrip(AttributeType attribute)
	{
		return roundTripAllValues(attribute).get(0);
	}

	private List<RoundTrippedValue> roundTripAllValues(AttributeType attribute)
	{
		AttributeDocument document = AttributeDocument.Factory.newInstance();
		document.setAttribute(attribute);
		try
		{
			AttributeDocument parsed = AttributeDocument.Factory.parse(document.xmlText());
			return Arrays.stream(parsed.getAttribute().getAttributeValueArray())
					.map(this::readRoundTrippedValue)
					.toList();
		} catch (XmlException e)
		{
			throw new AssertionError("Can not parse serialized SAML attribute", e);
		}
	}

	private RoundTrippedValue readRoundTrippedValue(XmlObject value)
	{
		Element element = (Element) value.getDomNode();
		String lexicalType = element.getAttributeNS(W3C_XML_SCHEMA_INSTANCE_NS_URI, "type");
		if (lexicalType.isBlank())
			throw new AssertionError("Serialized SAML attribute value has no xsi:type");
		String[] typeParts = lexicalType.split(":", 2);
		try (XmlCursor cursor = value.newCursor())
		{
			String namespace = typeParts.length == 2 ? cursor.namespaceForPrefix(typeParts[0]) : NULL_NS_URI;
			String localName = typeParts.length == 2 ? typeParts[1] : typeParts[0];
			return new RoundTrippedValue(new QName(namespace, localName), cursor.getTextValue());
		}
	}

	private record RoundTrippedValue(QName schemaType, String text)
	{
	}
}
