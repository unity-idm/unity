/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestIdentityParam;
import io.imunity.rest.api.types.basic.RestTypeBase;
import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementDecision;

public class RestEnquiryResponseStateTest extends RestTypeBase<RestEnquiryResponseState>
{
	@Override
	protected String getJson()
	{
		return "{\"Agreements\":[{\"selected\":true,\"externalIdp\":\"externalIdp\",\"translationProfile\":\"profile\"}],"
				+ "\"PolicyAgreements\":[{\"documentsIdsToAccept\":[1,2],\"acceptanceStatus\":\"ACCEPTED\"}],"
				+ "\"Attributes\":[{\"values\":[\"v1\"],\"name\":\"attr\",\"groupPath\":\"/\",\"valueSyntax\":\"syntax\"}],"
				+ "\"Comments\":\"comments\",\"Credentials\":[{\"credentialId\":\"credential\",\"secrets\":\"secret\"}],"
				+ "\"FormId\":\"formId\",\"GroupSelections\":[{\"selectedGroups\":[\"/g1\",\"/g2\"],"
				+ "\"externalIdp\":\"externalIdp\",\"translationProfile\":\"Profile\"}],"
				+ "\"Identities\":[{\"value\":\"test@wp.pl\",\"realm\":\"realm\","
				+ "\"target\":\"target\",\"translationProfile\":\"Profile\",\"remoteIdp\":\"remoteIdp\","
				+ "\"confirmationInfo\":{\"confirmed\":true,\"confirmationDate\":1,\"sentRequestAmount\":1},"
				+ "\"metadata\":{\"1\":\"v\"},\"typeId\":\"email\"}],\"UserLocale\":\"en\",\"RegistrationCode\":\"Code\","
				+ "\"AdminComments\":[{\"date\":1,\"contents\":\"comment\",\"authorEntityId\":1,"
				+ "\"publicComment\":true}],\"RequestId\":\"id\",\"Status\":\"pending\",\"Timestamp\":1,"
				+ "\"Context\":{\"isOnIdpEndpoint\":true,\"triggeringMode\":\"manualAtLogin\"},\"EntityId\":1}\n";
	}

	@Override
	protected RestEnquiryResponseState getObject()
	{

		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");

		return RestEnquiryResponseState.builder()
				.withAdminComments(List.of(RestAdminComment.builder()
						.withAuthorEntityId(1)
						.withContents("comment")
						.withDate(new Date(1))
						.withPublicComment(true)
						.build()))
				.withEntityId(1L)
				.withRegistrationContext(RestRegistrationContext.builder()
						.withIsOnIdpEndpoint(true)
						.withTriggeringMode("manualAtLogin")
						.build())
				.withRequestId("id")
				.withStatus("pending")
				.withTimestamp(new Date(1))
				.withRequest(RestEnquiryResponse.builder()
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
										.withConfirmationDate(1L)
										.build())
								.build()))
						.withPolicyAgreements(List.of(RestPolicyAgreementDecision.builder()
								.withAcceptanceStatus("ACCEPTED")
								.withDocumentsIdsToAccept(List.of(1L, 2L))
								.build()))
						.withRegistrationCode("Code")
						.withUserLocale("en")
						.build())

				.build();
	}
}
