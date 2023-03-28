/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.impl.attribute.DBAttribute;
import pl.edu.icm.unity.store.objstore.reg.common.DBGroupSelection;
import pl.edu.icm.unity.store.objstore.reg.common.DBIdentityParam;
import pl.edu.icm.unity.store.types.DBConfirmationInfo;

public class DBEnquiryInvitationParamTest extends DBTypeTestBase<DBEnquiryInvitationParam>
{
	@Override
	protected String getJson()
	{
		return "{\"type\":\"ENQUIRY\",\"expiration\":1,\"contactAddress\":\"contactAddress\",\"inviter\":1,\"entity\":1,"
				+ "\"formId\":\"formId\",\"identities\":{\"1\":{\"entry\":{\"value\":\"test@wp.pl\",\"realm\":\"realm\",\"target\":\"target\","
				+ "\"translationProfile\":\"Profile\",\"remoteIdp\":\"remoteIdp\",\"confirmationInfo\":{\"confirmed\":true,\"confirmationDate\":1,"
				+ "\"sentRequestAmount\":1},\"metadata\":{\"1\":\"v\"},\"typeId\":\"email\"},\"mode\":\"HIDDEN\"}},"
				+ "\"groupSelections\":{\"1\":{\"entry\":{\"selectedGroups\":[\"/g1\",\"/g2\"],\"externalIdp\":\"externalIdp\","
				+ "\"translationProfile\":\"Profile\"},\"mode\":\"READ_ONLY\"}},\"allowedGroups\":{\"1\":{\"selectedGroups\":[\"/g1\",\"/g2\"],"
				+ "\"externalIdp\":\"externalIdp\",\"translationProfile\":\"Profile\"}},\"attributes\":{\"1\":{\"entry\":{\"remoteIdp\":\"remoteIdp\","
				+ "\"translationProfile\":\"translationProfile\",\"values\":[\"v1\",\"v2\"],\"name\":\"attr\",\"groupPath\":\"/A\","
				+ "\"valueSyntax\":\"syntax\"},\"mode\":\"READ_ONLY\"}},\"messageParams\":{\"mpk1\":\"mpv1\"}}\n";

	}

	@Override
	protected DBEnquiryInvitationParam getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");

		return DBEnquiryInvitationParam.builder()
				.withEntity(1L)
				.withExpiration(1L)
				.withInviter(1L)
				.withType("ENQUIRY")
				.withContactAddress("contactAddress")
				.withFormPrefill(DBFormPrefill.builder()
						.withFormId("formId")
						.withAllowedGroups(Map.of(1, DBGroupSelection.builder()
								.withExternalIdp("externalIdp")
								.withTranslationProfile("Profile")
								.withSelectedGroups(List.of("/g1", "/g2"))
								.build()))
						.withAttributes(Map.of(1,
								new DBPrefilledEntry.Builder<DBAttribute>().withEntry(DBAttribute.builder()
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
								new DBPrefilledEntry.Builder<DBGroupSelection>()
										.withEntry(DBGroupSelection.builder()
												.withExternalIdp("externalIdp")
												.withTranslationProfile("Profile")
												.withSelectedGroups(List.of("/g1", "/g2"))
												.build())
										.withMode("READ_ONLY")
										.build()))
						.withMessageParams(Map.of("mpk1", "mpv1"))
						.withIdentities(Map.of(1,
								new DBPrefilledEntry.Builder<DBIdentityParam>()
										.withEntry(DBIdentityParam.builder()
												.withValue("test@wp.pl")
												.withTypeId("email")
												.withRealm("realm")
												.withRemoteIdp("remoteIdp")
												.withTarget("target")
												.withMetadata(meta)
												.withTranslationProfile("Profile")
												.withConfirmationInfo(DBConfirmationInfo.builder()
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
