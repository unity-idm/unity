/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.invite;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestIdentityParam;
import io.imunity.rest.api.types.basic.RestTypeBase;
import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import io.imunity.rest.api.types.registration.RestGroupSelection;

public class RestComboInvitationParamTest extends RestTypeBase<RestComboInvitationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"type\":\"COMBO\",\"expiration\":1,\"contactAddress\":\"contactAddress\",\"inviter\":1,"
				+ "\"registrationFormPrefill\":{\"formId\":\"formId\",\"identities\":{\"1\":{\"entry\":{\"value\":\"test@wp.pl\","
				+ "\"realm\":\"realm\",\"target\":\"target\",\"translationProfile\":\"Profile\",\"remoteIdp\":\"remoteIdp\","
				+ "\"confirmationInfo\":{\"confirmed\":true,\"confirmationDate\":1,\"sentRequestAmount\":1},"
				+ "\"metadata\":{\"1\":\"v\"},\"typeId\":\"email\"},\"mode\":\"HIDDEN\"}},"
				+ "\"groupSelections\":{\"1\":{\"entry\":{\"selectedGroups\":[\"/g1\",\"/g2\"],\"externalIdp\":\"externalIdp\","
				+ "\"translationProfile\":\"Profile\"},\"mode\":\"READ_ONLY\"}},"
				+ "\"allowedGroups\":{\"1\":{\"selectedGroups\":[\"/g1\",\"/g2\"],\"externalIdp\":\"externalIdp\","
				+ "\"translationProfile\":\"Profile\"}},\"attributes\":{\"1\":{\"entry\":{\"remoteIdp\":\"remoteIdp\","
				+ "\"translationProfile\":\"translationProfile\",\"values\":[\"v1\",\"v2\"],\"name\":\"attr\","
				+ "\"groupPath\":\"/A\",\"valueSyntax\":\"syntax\"},\"mode\":\"READ_ONLY\"}},\"messageParams\":{\"mpk1\":\"mpv1\"}},"
				+ "\"enquiryFormPrefill\":{\"formId\":\"formId2\",\"identities\":{\"1\":{\"entry\":{\"value\":\"test@wp.pl\","
				+ "\"realm\":\"realm\",\"target\":\"target\",\"translationProfile\":\"Profile\",\"remoteIdp\":\"remoteIdp\","
				+ "\"confirmationInfo\":{\"confirmed\":true,\"confirmationDate\":1,\"sentRequestAmount\":1},"
				+ "\"metadata\":{\"1\":\"v\"},\"typeId\":\"email\"},\"mode\":\"HIDDEN\"}},"
				+ "\"groupSelections\":{\"1\":{\"entry\":{\"selectedGroups\":[\"/g1\",\"/g2\"],\"externalIdp\":\"externalIdp\","
				+ "\"translationProfile\":\"Profile\"},\"mode\":\"READ_ONLY\"}},"
				+ "\"allowedGroups\":{\"1\":{\"selectedGroups\":[\"/g1\",\"/g2\"],\"externalIdp\":\"externalIdp\","
				+ "\"translationProfile\":\"Profile\"}},\"attributes\":{\"1\":{\"entry\":{\"remoteIdp\":\"remoteIdp\","
				+ "\"translationProfile\":\"translationProfile\",\"values\":[\"v1\",\"v2\"],\"name\":\"attr\","
				+ "\"groupPath\":\"/A\",\"valueSyntax\":\"syntax\"},\"mode\":\"READ_ONLY\"}},"
				+ "\"messageParams\":{\"mpk1\":\"mpv1\"}}}\n";
	}

	@Override
	protected RestComboInvitationParam getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");

		return RestComboInvitationParam.builder()
				.withExpiration(1L)
				.withInviter(1L)
				.withType("COMBO")
				.withContactAddress("contactAddress")
				.withEnquiryFormPrefill(RestFormPrefill.builder()
						.withFormId("formId2")
						.withAllowedGroups(Map.of(1, RestGroupSelection.builder()
								.withExternalIdp("externalIdp")
								.withTranslationProfile("Profile")
								.withSelectedGroups(List.of("/g1", "/g2"))
								.build()))
						.withAttributes(Map.of(1,
								new RestPrefilledEntry.Builder<RestAttribute>().withEntry(RestAttribute.builder()
										.withName("attr")
										.withValueSyntax("syntax")
										.withGroupPath("/A")
										.withValues(List.of("v1", "v2"))
										.withRemoteIdp("remoteIdp")
										.withTranslationProfile("translationProfile")
										.build())
										.withMode("READ_ONLY")
										.build()))
						.withGroupSelections(Map.of(1,
								new RestPrefilledEntry.Builder<RestGroupSelection>()
										.withEntry(RestGroupSelection.builder()
												.withExternalIdp("externalIdp")
												.withTranslationProfile("Profile")
												.withSelectedGroups(List.of("/g1", "/g2"))
												.build())
										.withMode("READ_ONLY")
										.build()))
						.withMessageParams(Map.of("mpk1", "mpv1"))
						.withIdentities(Map.of(1,
								new RestPrefilledEntry.Builder<RestIdentityParam>()
										.withEntry(RestIdentityParam.builder()
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
												.build())
										.withMode("HIDDEN")
										.build()))
						.build())
				.withRegistrationFormPrefill(RestFormPrefill.builder()
						.withFormId("formId")
						.withAllowedGroups(Map.of(1, RestGroupSelection.builder()
								.withExternalIdp("externalIdp")
								.withTranslationProfile("Profile")
								.withSelectedGroups(List.of("/g1", "/g2"))
								.build()))
						.withAttributes(Map.of(1,
								new RestPrefilledEntry.Builder<RestAttribute>().withEntry(RestAttribute.builder()
										.withName("attr")
										.withValueSyntax("syntax")
										.withGroupPath("/A")
										.withValues(List.of("v1", "v2"))
										.withRemoteIdp("remoteIdp")
										.withTranslationProfile("translationProfile")
										.build())
										.withMode("READ_ONLY")
										.build()))
						.withGroupSelections(Map.of(1,
								new RestPrefilledEntry.Builder<RestGroupSelection>()
										.withEntry(RestGroupSelection.builder()
												.withExternalIdp("externalIdp")
												.withTranslationProfile("Profile")
												.withSelectedGroups(List.of("/g1", "/g2"))
												.build())
										.withMode("READ_ONLY")
										.build()))
						.withMessageParams(Map.of("mpk1", "mpv1"))
						.withIdentities(Map.of(1,
								new RestPrefilledEntry.Builder<RestIdentityParam>()
										.withEntry(RestIdentityParam.builder()
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
												.build())
										.withMode("HIDDEN")
										.build()))
						.build())
				.build();
	}

}
