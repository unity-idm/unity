/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.saml.idp.GroupChooser;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAttributeQueryContext;
import xmlbeans.org.oasis.saml2.assertion.AttributeDocument;
import xmlbeans.org.oasis.saml2.protocol.AttributeQueryDocument;

public class AttributeQueryResponseProcessorTest
{
	private static final String ATTRIBUTE_PREFIX = """
			<saml:Attribute xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
					xmlns:xs="http://www.w3.org/2001/XMLSchema"
					xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
					Name="attribute">
			""";
	private static final String ATTRIBUTE_SUFFIX = "</saml:Attribute>";

	private AttributeQueryResponseProcessor processor;

	@BeforeEach
	public void setUp()
	{
		SAMLIdPConfiguration configuration = mock(SAMLIdPConfiguration.class);
		when(configuration.getGroupChooser()).thenReturn(mock(GroupChooser.class));
		AttributeQueryDocument request = AttributeQueryDocument.Factory.newInstance();
		request.addNewAttributeQuery().addNewIssuer().setStringValue("issuer");
		SAMLAttributeQueryContext context = new SAMLAttributeQueryContext(request, configuration);
		processor = new AttributeQueryResponseProcessor(null, context);
	}

	@Test
	public void shouldMatchSimpleValuesWithEqualText()
	{
		XmlObject tested = parseValue("<saml:AttributeValue xsi:type=\"xs:double\">123.1</saml:AttributeValue>");
		XmlObject permitted = parseValue("<saml:AttributeValue xsi:type=\"xs:string\">123.1</saml:AttributeValue>");

		boolean result = processor.isAmongValues(tested, new XmlObject[] {permitted});

		assertThat(result).isTrue();
	}

	@Test
	public void shouldRejectSimpleValuesWithDifferentText()
	{
		XmlObject tested = parseValue("<saml:AttributeValue>admin</saml:AttributeValue>");
		XmlObject permitted = parseValue("<saml:AttributeValue>user</saml:AttributeValue>");

		boolean result = processor.isAmongValues(tested, new XmlObject[] {permitted});

		assertThat(result).isFalse();
	}

	@Test
	public void shouldRejectNilValueMatchingEmptyString()
	{
		XmlObject tested = parseValue("<saml:AttributeValue xsi:type=\"xs:string\"></saml:AttributeValue>");
		XmlObject permitted = parseValue("<saml:AttributeValue xsi:nil=\"true\"/>");

		boolean result = processor.isAmongValues(tested, new XmlObject[] {permitted});

		assertThat(result).isFalse();
	}

	@Test
	public void shouldRejectStructuredValueMatchingScalarText()
	{
		XmlObject tested = parseValue("<saml:AttributeValue>admin</saml:AttributeValue>");
		XmlObject permitted = parseValue("<saml:AttributeValue><role>admin</role></saml:AttributeValue>");

		boolean result = processor.isAmongValues(tested, new XmlObject[] {permitted});

		assertThat(result).isFalse();
	}

	@Test
	public void shouldRejectMatchingStructuredValues()
	{
		XmlObject tested = parseValue("<saml:AttributeValue><role>admin</role></saml:AttributeValue>");
		XmlObject permitted = parseValue("<saml:AttributeValue><role>admin</role></saml:AttributeValue>");

		boolean result = processor.isAmongValues(tested, new XmlObject[] {permitted});

		assertThat(result).isFalse();
	}

	private XmlObject parseValue(String value)
	{
		try
		{
			AttributeDocument document = AttributeDocument.Factory.parse(ATTRIBUTE_PREFIX + value + ATTRIBUTE_SUFFIX);
			return document.getAttribute().getAttributeValueArray(0);
		} catch (XmlException e)
		{
			throw new AssertionError("Can not parse SAML attribute value", e);
		}
	}
}
