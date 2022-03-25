/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.emi.security.authn.x509.X509Credential;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;

public class FakeSAMLSPConfiguration
{
	public static SAMLSPConfiguration.Builder getFakeBuilder()
	{
		return SAMLSPConfiguration.builder()
				.withRequesterSamlId("http://unity/as/sp")
				.withSignRequestByDefault(true)
				.withMetadataURLPath("meta")
				.withRequesterCredential(mock(X509Credential.class))
				.withTrustChecker(new TrustAllTrustChecker())
				.withAcceptedNameFormats(List.of())
				.withEffectiveMappings(Map.of())
				.withIndividualTrustedIdPs(new TrustedIdPs(Set.of()));
	}
	
	public static SAMLSPConfiguration get()
	{
		return getFakeBuilder().build();
	}
}
