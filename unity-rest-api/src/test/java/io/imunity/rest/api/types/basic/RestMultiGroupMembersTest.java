/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.imunity.rest.api.types.authn.RestCredentialInfo;
import io.imunity.rest.api.types.authn.RestCredentialPublicInformation;
import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;

public class RestMultiGroupMembersTest extends RestTypeBase<RestMultiGroupMembers>
{

	@Override
	protected String getJson()
	{
		return "{\"entities\":[{\"entityInformation\":{\"state\":\"valid\",\"ScheduledOperationTime\":1,"
				+ "\"ScheduledOperation\":\"DISABLE\",\"RemovalByUserTime\":1,\"entityId\":1},"
				+ "\"identities\":[{\"value\":\"test@wp.pl\",\"realm\":\"realm\",\"target\":\"target\","
				+ "\"translationProfile\":\"Profile\",\"remoteIdp\":\"remoteIdp\",\"confirmationInfo\":{\"confirmed\":true,"
				+ "\"confirmationDate\":1,\"sentRequestAmount\":1},\"metadata\":{\"1\":\"v\"},"
				+ "\"comparableValue\":\"test@wp.pl\",\"creationTs\":1,\"updateTs\":1,"
				+ "\"typeId\":\"email\",\"entityId\":1}],\"credentialInfo\":{\"credentialRequirementId\":\"credreq1\","
				+ "\"credentialsState\":{\"test\":{\"state\":\"correct\",\"stateDetail\":\"state\","
				+ "\"extraInformation\":\"state\"}}}}],\"members\":{\"/\":[{\"entityId\":1,"
				+ "\"attributes\":[{\"remoteIdp\":\"remoteIdp\",\"translationProfile\":\"translationProfile\","
				+ "\"values\":[\"v1\",\"v2\"],\"creationTs\":100,\"updateTs\":1000,\"direct\":true,"
				+ "\"name\":\"attr\",\"groupPath\":\"/A\",\"valueSyntax\":\"syntax\"}]}]}}\n";
	}

	@Override
	protected RestMultiGroupMembers getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");

		return RestMultiGroupMembers.builder()
				.withEntities(List.of(RestEntity.builder()
						.withCredentialInfo(RestCredentialInfo.builder()
								.withCredentialRequirementId("credreq1")
								.withCredentialsState(Map.of("test", RestCredentialPublicInformation.builder()
										.withState("correct")
										.withStateDetail("state")
										.withExtraInformation("state")
										.build()))
								.build())
						.withEntityInformation(RestEntityInformation.builder()
								.withEntityId(1L)
								.withState("valid")
								.withRemovalByUserTime(new Date(1))
								.withScheduledOperationTime(new Date(1))
								.withScheduledOperation("DISABLE")
								.build())
						.withIdentities(List.of(RestIdentity.builder()
								.withCreationTs(new Date(1))
								.withUpdateTs(new Date(1))
								.withComparableValue("test@wp.pl")
								.withValue("test@wp.pl")
								.withEntityId(1L)
								.withTypeId("email")
								.withRealm("realm")
								.withRemoteIdp("remoteIdp")
								.withTarget("target")
								.withMetadata(meta)
								.withTranslationProfile("Profile")
								.withConfirmationInfo(RestConfirmationInfo.builder()
										.withSentRequestAmount(1)
										.withConfirmed(true)
										.withConfirmationDate(1)
										.build())
								.build()))
						.build()))
				.withMembers(Map.of("/", List.of(RestEntityGroupAttributes.builder()
						.withEntityId(1)
						.withAttributes(List.of(RestAttributeExt.builder()
								.withRemoteIdp("remoteIdp")
								.withTranslationProfile("translationProfile")
								.withGroupPath("/A")
								.withValueSyntax("syntax")
								.withValues(List.of("v1", "v2"))
								.withName("attr")
								.withCreationTs(new Date(100L))
								.withUpdateTs(new Date(1000L))
								.withDirect(true)
								.build()))
						.build())))

				.build();
	}

}
