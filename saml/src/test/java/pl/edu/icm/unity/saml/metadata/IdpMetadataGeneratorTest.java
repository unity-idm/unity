/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;


public class IdpMetadataGeneratorTest
{
	private SAMLIdPConfiguration samlIdpConfiguration;
	private MessageSource messageSource;

	@BeforeEach
	public void setUp() throws Exception
	{
		PKIManagement pkiManagement = mock(PKIManagement.class);
		when(pkiManagement.getCredentialNames()).thenReturn(Set.of("foo"));
		samlIdpConfiguration = SAMLIdPConfiguration.builder()
				.withCredentialName("foo")
				.withGroupChooser(Map.of(), "foo")
				.withIssuerURI("foo")
				.build();

		messageSource = mock(MessageSource.class);
		when(messageSource.getDefaultLocaleCode()).thenReturn("en");
	}

	@Test
	public void shouldAddEngAndPlOrganizationDisplayNames()
	{
		I18nString displayedNames = new I18nString();
		displayedNames.addValue("en", "Ala has a cat");
		displayedNames.addValue("pl", "Ala ma kota");

		IdpMetadataGenerator generator = new IdpMetadataGenerator(samlIdpConfiguration,
				new EndpointType[0], new EndpointType[0], new EndpointType[0], displayedNames, messageSource);

		String xmlMetadata = generator.getMetadata().xmlText();
		assertNotNull(xmlMetadata);
		assertThat(xmlMetadata).contains("<urn:OrganizationDisplayName xml:lang=\"pl\">Ala ma kota</urn:OrganizationDisplayName>");
		assertThat(xmlMetadata).contains("<urn:OrganizationDisplayName xml:lang=\"en\">Ala has a cat</urn:OrganizationDisplayName>");
	}

	@Test
	public void shouldAddDefaultOrganizationDisplayNames()
	{
		I18nString displayedNames = new I18nString("Ala has a cat");

		IdpMetadataGenerator generator = new IdpMetadataGenerator(samlIdpConfiguration,
				new EndpointType[0], new EndpointType[0], new EndpointType[0], displayedNames, messageSource);

		String xmlMetadata = generator.getMetadata().xmlText();
		assertNotNull(xmlMetadata);
		assertThat(xmlMetadata).contains("<urn:OrganizationDisplayName xml:lang=\"en\">Ala has a cat</urn:OrganizationDisplayName>");
	}
}
