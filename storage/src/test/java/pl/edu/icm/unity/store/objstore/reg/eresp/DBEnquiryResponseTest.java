/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.eresp;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;


import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.impl.attribute.DBAttribute;
import pl.edu.icm.unity.store.objstore.reg.common.DBCredentialParamValue;
import pl.edu.icm.unity.store.objstore.reg.common.DBGroupSelection;
import pl.edu.icm.unity.store.objstore.reg.common.DBIdentityParam;
import pl.edu.icm.unity.store.objstore.reg.common.DBPolicyAgreementDecision;
import pl.edu.icm.unity.store.objstore.reg.common.DBSelection;
import pl.edu.icm.unity.store.types.common.DBConfirmationInfo;

public class DBEnquiryResponseTest extends DBTypeTestBase<DBEnquiryResponse>
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
	protected DBEnquiryResponse getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");

		return DBEnquiryResponse.builder()
				.withAgreements(List.of(DBSelection.builder()
						.withExternalIdp("externalIdp")
						.withSelected(true)
						.withTranslationProfile("profile")
						.build()))
				.withAttributes(List.of(DBAttribute.builder()
						.withName("attr")
						.withValueSyntax("syntax")
						.withGroupPath("/")
						.withValues(List.of("v1"))
						.build()))
				.withComments("comments")
				.withCredentials(List.of(DBCredentialParamValue.builder()
						.withCredentialId("credential")
						.withSecrets("secret")
						.build()))
				.withFormId("formId")
				.withGroupSelections(List.of(DBGroupSelection.builder()
						.withExternalIdp("externalIdp")
						.withTranslationProfile("Profile")
						.withSelectedGroups(List.of("/g1", "/g2"))
						.build()))
				.withIdentities(List.of(DBIdentityParam.builder()
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
								.withConfirmationDate(1673699833200L)
								.build())
						.build()))
				.withPolicyAgreements(List.of(DBPolicyAgreementDecision.builder()
						.withAcceptanceStatus("ACCEPTED")
						.withDocumentsIdsToAccept(List.of(1L, 2L))
						.build()))
				.withRegistrationCode("Code")
				.withUserLocale("en")
				.build();
	}

}
