/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.types.I18nString;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;

import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.saml.SamlProperties.P;


public class IdpMetadataGeneratorTest
{
	private SamlIdpProperties samlIdpProperties;
	private MessageSource messageSource;

	@Before
	public void setUp() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P+"credential", "foo");
		p.setProperty(P+"defaultGroup", "foo");
		p.setProperty(P+"issuerURI", "foo");

		PKIManagement pkiManagement = mock(PKIManagement.class);
		when(pkiManagement.getCredentialNames()).thenReturn(Set.of("foo"));
		samlIdpProperties = new SamlIdpProperties(p, pkiManagement);

		messageSource = mock(MessageSource.class);
		when(messageSource.getDefaultLocaleCode()).thenReturn("en");
	}

	@Test
	public void shouldAddEngAndPlOrganizationDisplayNames()
	{
		I18nString displayedNames = new I18nString();
		displayedNames.addValue("en", "Ala has a cat");
		displayedNames.addValue("pl", "Ala ma kota");

		IdpMetadataGenerator generator = new IdpMetadataGenerator(samlIdpProperties,
				new EndpointType[0], new EndpointType[0], new EndpointType[0], displayedNames, messageSource);

		String xmlMetadata = generator.getMetadata().xmlText();
		Assert.assertNotNull(xmlMetadata);
		assertThat(xmlMetadata).contains("<urn:OrganizationDisplayName xml:lang=\"pl\">Ala ma kota</urn:OrganizationDisplayName>");
		assertThat(xmlMetadata).contains("<urn:OrganizationDisplayName xml:lang=\"en\">Ala has a cat</urn:OrganizationDisplayName>");
	}

	@Test
	public void shouldAddDefaultOrganizationDisplayNames()
	{
		I18nString displayedNames = new I18nString("Ala has a cat");

		IdpMetadataGenerator generator = new IdpMetadataGenerator(samlIdpProperties,
				new EndpointType[0], new EndpointType[0], new EndpointType[0], displayedNames, messageSource);

		String xmlMetadata = generator.getMetadata().xmlText();
		Assert.assertNotNull(xmlMetadata);
		assertThat(xmlMetadata).contains("<urn:OrganizationDisplayName xml:lang=\"en\">Ala has a cat</urn:OrganizationDisplayName>");
	}
}
