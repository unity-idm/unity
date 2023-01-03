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

public class RestEntityWithAttributesTest extends RestTypeBase<RestEntityWithAttributes>
{

	@Override
	protected String getJson()
	{
		return "{\"entity\":{\"entityInformation\":{\"state\":\"valid\",\"ScheduledOperationTime\":1670235756509,\"ScheduledOperation\":\"DISABLE\","
				+ "\"RemovalByUserTime\":1670235756509,\"entityId\":1},\"identities\":[{\"value\":\"test@wp.pl\",\"realm\":\"real\",\"target\":\"target\","
				+ "\"translationProfile\":\"Profile\",\"remoteIdp\":\"remoteIdp\",\"confirmationInfo\":{\"confirmed\":true,\"confirmationDate\":1670235756247,"
				+ "\"sentRequestAmount\":1},\"metadata\":{\"1\":\"v\"},\"comparableValue\":\"test@wp.pl\",\"creationTs\":1670235756247,\"updateTs\":1670235756247,"
				+ "\"typeId\":\"email\",\"entityId\":1}],\"credentialInfo\":{\"credentialRequirementId\":\"credreq1\","
				+ "\"credentialsState\":{\"test\":{\"state\":\"correct\",\"stateDetail\":\"state\","
				+ "\"extraInformation\":\"state\"}}}},\"groups\":{\"/\":{\"remoteIdp\":\"remoteIdp\",\"translationProfile\":\"profile\","
				+ "\"creationTs\":1,\"group\":\"/\",\"entityId\":1}},\"attributesInGroups\":{\"/\":[{\"remoteIdp\":\"remoteIdp\","
				+ "\"translationProfile\":\"translationProfile\",\"values\":[\"v1\",\"v2\"],\"creationTs\":100,\"updateTs\":1000,\"direct\":true,"
				+ "\"name\":\"attr\",\"groupPath\":\"/A\",\"valueSyntax\":\"syntax\",\"simpleValues\":[\"v1s\",\"v2s\"]}]}}";
	}

	@Override
	protected RestEntityWithAttributes getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");
		return new RestEntityWithAttributes(RestEntity.builder()
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
						.withRemovalByUserTime(new Date(1670235756509L))
						.withScheduledOperationTime(new Date(1670235756509L))
						.withScheduledOperation("DISABLE")
						.build())
				.withIdentities(List.of(RestIdentity.builder()
						.withCreationTs(new Date(1670235756247L))
						.withUpdateTs(new Date(1670235756247L))
						.withComparableValue("test@wp.pl")
						.withValue("test@wp.pl")
						.withEntityId(1L)
						.withTypeId("email")
						.withRealm("real")
						.withRemoteIdp("remoteIdp")
						.withTarget("target")
						.withMetadata(meta)
						.withTranslationProfile("Profile")
						.withConfirmationInfo(RestConfirmationInfo.builder()
								.withSentRequestAmount(1)
								.withConfirmed(true)
								.withConfirmationDate(1670235756247L)
								.build())
						.build()))
				.build(),
				Map.of("/", RestGroupMembership.builder()
						.withCreationTs(new Date(1))
						.withEntityId(1)
						.withGroup("/")
						.withRemoteIdp("remoteIdp")
						.withTranslationProfile("profile")
						.build()),
				Map.of("/", List.of(RestExternalizedAttribute.builder()
						.withRemoteIdp("remoteIdp")
						.withTranslationProfile("translationProfile")
						.withGroupPath("/A")
						.withValueSyntax("syntax")
						.withValues(List.of("v1", "v2"))
						.withName("attr")
						.withCreationTs(new Date(100L))
						.withUpdateTs(new Date(1000L))
						.withDirect(true)
						.withSimpleValues(List.of("v1s", "v2s"))
						.build())));
	}

}
