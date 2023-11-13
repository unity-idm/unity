/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestIdentityParam;
import io.imunity.rest.api.types.basic.RestTypeBase;
import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementDecision;

public class RestEnquiryResponseTest extends RestTypeBase<RestEnquiryResponse>
{

	@Override
	protected String getJson()
	{
		return "{\"Agreements\":[{\"selected\":true,\"externalIdp\":\"externalIdp\",\"translationProfile\":\"profile\"}]"
				+ ",\"PolicyAgreements\":[{\"documentsIdsToAccept\":[1,2],\"acceptanceStatus\":\"ACCEPTED\"}],"
				+ "\"Attributes\":[{\"values\":[\"v1\"],\"name\":\"attr\",\"groupPath\":\"/\","
				+ "\"valueSyntax\":\"syntax\"}],\"Comments\":\"comments\","
				+ "\"Credentials\":[{\"credentialId\":\"credential\",\"secrets\":\"secret\"}],\"FormId\":\"formId\","
				+ "\"GroupSelections\":[{\"selectedGroups\":[\"/g1\",\"/g2\"],\"externalIdp\":\"externalIdp\","
				+ "\"translationProfile\":\"Profile\"}],\"Identities\":[{\"value\":\"test@wp.pl\",\"realm\":\"realm\","
				+ "\"target\":\"target\",\"translationProfile\":\"Profile\",\"remoteIdp\":\"remoteIdp\","
				+ "\"confirmationInfo\":{\"confirmed\":true,\"confirmationDate\":1673699833200,"
				+ "\"sentRequestAmount\":1},\"metadata\":{\"1\":\"v\"},\"typeId\":\"email\"}],\"UserLocale\":\"en\","
				+ "\"RegistrationCode\":\"Code\"}\n";
	}

	@Override
	protected RestEnquiryResponse getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");

		return RestEnquiryResponse.builder()
				.withAgreements(List.of(RestSelection.builder()
						.withExternalIdp("externalIdp")
						.withSelected(true)
						.withTranslationProfile("profile")
						.build()))
				.withAttributes(List.of(RestAttribute.builder()
						.withName("attr")
						.withValueSyntax("syntax")
						.withGroupPath("/")
						.withValues(List.of("v1"))
						.build()))
				.withComments("comments")
				.withCredentials(List.of(RestCredentialParamValue.builder()
						.withCredentialId("credential")
						.withSecrets("secret")
						.build()))
				.withFormId("formId")
				.withGroupSelections(List.of(RestGroupSelection.builder()
						.withExternalIdp("externalIdp")
						.withTranslationProfile("Profile")
						.withSelectedGroups(List.of("/g1", "/g2"))
						.build()))
				.withIdentities(List.of(RestIdentityParam.builder()
						.withValue("test@wp.pl")
						.withTypeId("email")
						.withRealm("realm")
						.withRemoteIdp("remoteIdp")
						.withTarget("target")
						.withMetadata(meta)
						.withTranslationProfile("Profile")
						.withConfirmationInfo(RestConfirmationInfo.builder()
								.withSentRequestAmount(1)
								.withConfirmed(true)
								.withConfirmationDate(1673699833200L)
								.build())
						.build()))
				.withPolicyAgreements(List.of(RestPolicyAgreementDecision.builder()
						.withAcceptanceStatus("ACCEPTED")
						.withDocumentsIdsToAccept(List.of(1L, 2L))
						.build()))
				.withRegistrationCode("Code")
				.withUserLocale("en")
				.build();
	}

}
