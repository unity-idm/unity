/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.TrustedServiceProvider;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

public class MetaToIDPConfigConvertTest
{
	@Test
	public void shouldGetAlsoIgnoredSPs() throws EngineException
	{
		PKIManagement pkiManagement = mock(PKIManagement.class);
		when(pkiManagement.getCertificate(any())).thenReturn(new NamedCertificate("cert", mock(X509Certificate.class)));
		MessageSource msg = mock(MessageSource.class);

		MetaToIDPConfigConverter converter = new MetaToIDPConfigConverter(pkiManagement, msg);
		EntitiesDescriptorDocument metadata = EntitiesDescriptorDocumentParser
				.loadMetadata("src/test/resources/DFN-AAI-metadata-part-hidden.xml");

		SAMLIdPConfiguration samlIdPConfiguration = SAMLIdPConfiguration.builder()
				.withCredentialName("MAIN")
				.withIssuerURI("me")
				.withGroupChooser(Map.of("group", "group"), "group")
				.build();
		Set<TrustedServiceProvider> trustedSps = converter.convertToTrustedSps(metadata, samlIdPConfiguration);
		assertThat(trustedSps.stream()
				.map(sp -> sp.entityId.id)
				.toList()).contains("http://shibboleth.metapress.com/shibboleth-sp-hidden");
		assertThat(trustedSps.size()).isEqualTo(2);
	}
}
