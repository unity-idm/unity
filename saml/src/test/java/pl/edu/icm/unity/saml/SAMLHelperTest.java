/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.authn.RequestedAuthenticationContextClassReference;
import pl.edu.icm.unity.saml.sp.config.ComparisonMethod;
import pl.edu.icm.unity.saml.sp.config.RequestACRsMode;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.RequestedAuthnContextType;

public class SAMLHelperTest
{

	@Test
	public void shouldAddForwardedACRs()
	{
		AuthnRequestDocument samlRequest = SAMLHelper.createSAMLRequest(null, false, null, null, null, false, null,
				new RequestedAuthenticationContextClassReference(List.of("ess1"), List.of("vol1")),
				SAMLSPConfiguration.builder()
						.withRequesterSamlId("id")
						.withAcceptedNameFormats(List.of())
						.withEffectiveMappings(Map.of())
						.withIndividualTrustedIdPs(new TrustedIdPs(List.of()))
						.withRequestedACRs(List.of())
						.withRequestACRsMode(RequestACRsMode.FORWARD)
						.build());

		assertThat(samlRequest.getAuthnRequest()
				.getRequestedAuthnContext()).isNotNull();
		RequestedAuthnContextType requestedAuthnContext = samlRequest.getAuthnRequest()
				.getRequestedAuthnContext();
		assertThat(requestedAuthnContext.getComparison())
				.isEqualTo(xmlbeans.org.oasis.saml2.protocol.AuthnContextComparisonType.EXACT);
		assertThat(requestedAuthnContext.getAuthnContextClassRefArray()).isEqualTo(List.of("ess1", "vol1")
				.toArray(String[]::new));
	}
	
	@Test
	public void shouldNotAddForwardedACRsIfAreEmpty()
	{
		AuthnRequestDocument samlRequest = SAMLHelper.createSAMLRequest(null, false, null, null, null, false, null,
				new RequestedAuthenticationContextClassReference(List.of(), List.of()),
				SAMLSPConfiguration.builder()
						.withRequesterSamlId("id")
						.withAcceptedNameFormats(List.of())
						.withEffectiveMappings(Map.of())
						.withIndividualTrustedIdPs(new TrustedIdPs(List.of()))
						.withRequestedACRs(List.of())
						.withRequestACRsMode(RequestACRsMode.FORWARD)
						.build());

		assertThat(samlRequest.getAuthnRequest()
				.getRequestedAuthnContext()).isNull();		
	}

	@Test
	public void shouldAddFixedACRs()
	{
		AuthnRequestDocument samlRequest = SAMLHelper.createSAMLRequest(null, false, null, null, null, false, null,
				new RequestedAuthenticationContextClassReference(List.of(), List.of()), SAMLSPConfiguration.builder()
						.withRequesterSamlId("id")
						.withAcceptedNameFormats(List.of())
						.withEffectiveMappings(Map.of())
						.withIndividualTrustedIdPs(new TrustedIdPs(List.of()))
						.withRequestedACRs(List.of("acr1", "acr2"))
						.withComparisonMethod(ComparisonMethod.BETTER)
						.withRequestACRsMode(RequestACRsMode.FIXED)
						.build());

		assertThat(samlRequest.getAuthnRequest()
				.getRequestedAuthnContext()).isNotNull();
		RequestedAuthnContextType requestedAuthnContext = samlRequest.getAuthnRequest()
				.getRequestedAuthnContext();
		assertThat(requestedAuthnContext.getComparison())
				.isEqualTo(xmlbeans.org.oasis.saml2.protocol.AuthnContextComparisonType.BETTER);
		assertThat(requestedAuthnContext.getAuthnContextClassRefArray()).isEqualTo(List.of("acr1", "acr2")
				.toArray(String[]::new));
	}
}
