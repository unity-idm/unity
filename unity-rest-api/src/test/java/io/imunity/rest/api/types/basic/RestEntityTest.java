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

public class RestEntityTest extends RestTypeBase<RestEntity>
{
	protected String getJson()
	{
		return "{\"entityInformation\":{\"state\":\"valid\",\"ScheduledOperationTime\":1666788129805,\"ScheduledOperation\":\"DISABLE\",\"RemovalByUserTime\":1666788129805,\"entityId\":1},\"identities\":[{\"value\":\"test@wp.pl\",\"realm\":\"realm\",\"target\":\"target\",\"translationProfile\":\"Profile\",\"remoteIdp\":\"remoteIdp\",\"confirmationInfo\":{\"confirmed\":true,\"confirmationDate\":1666788129804,\"sentRequestAmount\":1},\"comparableValue\":\"test@wp.pl\",\"creationTs\":1666788129804,\"updateTs\":1666788129804,\"typeId\":\"email\",\"entityId\":1, \"metadata\":{\"1\":\"v\"}}],\"credentialInfo\":{\"credentialRequirementId\":\"credreq1\",\"credentialsState\":{\"test\":{\"state\":\"correct\",\"stateDetail\":\"state\",\"extraInformation\":\"state\"}}}}\n";

	}

	protected RestEntity getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");

		return RestEntity.builder()
				.withCredentialInfo(RestCredentialInfo.builder().withCredentialRequirementId("credreq1")
						.withCredentialsState(Map.of("test",
								RestCredentialPublicInformation.builder().withState("correct").withStateDetail("state")
										.withExtraInformation("state").build()))
						.build())
				.withEntityInformation(RestEntityInformation.builder().withEntityId(1L).withState("valid")
						.withRemovalByUserTime(new Date(1666788129805L))
						.withScheduledOperationTime(new Date(1666788129805L)).withScheduledOperation("DISABLE").build())
				.withIdentities(
						List.of(RestIdentity.builder().withCreationTs(new Date(1666788129804L))
								.withUpdateTs(new Date(1666788129804L)).withComparableValue("test@wp.pl")
								.withValue("test@wp.pl").withEntityId(1L).withTypeId("email").withRealm("realm")
								.withRemoteIdp("remoteIdp").withTarget("target").withMetadata(meta)
								.withTranslationProfile("Profile")
								.withConfirmationInfo(RestConfirmationInfo.builder().withSentRequestAmount(1)
										.withConfirmed(true).withConfirmationDate(1666788129804L).build())
								.build()))
				.build();

	}

}
