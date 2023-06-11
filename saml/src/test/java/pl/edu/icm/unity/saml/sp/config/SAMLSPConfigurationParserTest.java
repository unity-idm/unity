/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.CREDENTIAL;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.DEF_SIGN_REQUEST;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDPMETA_PREFIX;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.METADATA_PATH;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.P;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.REQUESTER_ID;

import java.util.Properties;

import org.junit.Test;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.saml.SamlProperties;

public class SAMLSPConfigurationParserTest
{
	@Test
	public void shouldParseTrustedFederationMetadata()
	{
		Properties p = new Properties();
		p.setProperty(P+REQUESTER_ID, "foo");
		p.setProperty(P+DEF_SIGN_REQUEST, "true");
		p.setProperty(P+CREDENTIAL, "MAIN");
		p.setProperty(P+METADATA_PATH, "meta");
		p.setProperty(P+IDPMETA_PREFIX+"M1."+SamlProperties.METADATA_URL, "https://example.com/path");
		p.setProperty(P+IDPMETA_PREFIX+"M1."+SamlProperties.METADATA_REFRESH, "100");
		
		SAMLSPConfigurationParser parser = new SAMLSPConfigurationParser(
				mock(PKIManagement.class), mock(MessageSource.class));
		
		SAMLSPConfiguration parsed = parser.parse(p);
		
		assertThat(parsed.trustedMetadataSourcesByUrl).hasSize(1).containsKey("https://example.com/path");
		assertThat(parsed.trustedMetadataSourcesByUrl.get("https://example.com/path").url)
			.isEqualTo("https://example.com/path");
		assertThat(parsed.trustedMetadataSourcesByUrl.get("https://example.com/path").translationProfile)
			.isNotNull();
	}
}
