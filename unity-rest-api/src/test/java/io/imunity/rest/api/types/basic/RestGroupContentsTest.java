/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class RestGroupContentsTest extends RestTypeBase<RestGroupContents>
{

	@Override
	protected String getJson()
	{
		return "{\"group\":{\"i18nDescription\":{\"DefaultValue\":\"desc\",\"Map\":{}},\"displayedName\":{\"DefaultValue\":\"groupDisp\",\"Map\":{}},\"attributeStatements\":[{\"resolution\":\"merge\",\"condition\":\"cond\",\"extraGroupName\":\"/extra\",\"fixedAttribute\":{\"values\":[\"v1\"],\"name\":\"attr\",\"groupPath\":\"/\",\"valueSyntax\":\"string\"}}],\"attributesClasses\":[\"attrClass2\",\"attrClass1\"],\"delegationConfiguration\":{\"enabled\":false,\"enableSubprojects\":false,\"logoUrl\":null,\"registrationForm\":null,\"signupEnquiryForm\":null,\"membershipUpdateEnquiryForm\":null,\"attributes\":null},\"properties\":[{\"key\":\"key\",\"value\":\"value\"}],\"publicGroup\":false,\"path\":\"/A/B/C\"},\"subGroups\":[\"/A\",\"/B\"],\"members\":[{\"remoteIdp\":\"remoteIdp\",\"translationProfile\":\"profile\",\"creationTs\":1,\"group\":\"/\",\"entityId\":1}]}";
	}

	@Override
	protected RestGroupContents getObject()
	{
		return RestGroupContents.builder()
				.withGroup(RestGroup.builder()
						.withPath("/A/B/C")
						.withAttributesClasses(Set.of("attrClass1", "attrClass2"))
						.withAttributeStatements(new RestAttributeStatement[]
						{ RestAttributeStatement.builder()
								.withCondition("cond")
								.withResolution("merge")
								.withExtraGroupName("/extra")
								.withFixedAttribute(RestAttribute.builder()
										.withName("attr")
										.withGroupPath("/")
										.withValueSyntax("string")
										.withValues(List.of("v1"))
										.build())
								.build() })
						.withDisplayedName(RestI18nString.builder()
								.withDefaultValue("groupDisp")
								.build())
						.withI18nDescription(RestI18nString.builder()
								.withDefaultValue("desc")
								.build())
						.withPublicGroup(false)
						.withDelegationConfiguration(RestGroupDelegationConfiguration.builder()
								.withEnabled(false)
								.withEnableSubprojects(false)
								.build())
						.withProperties(List.of(RestGroupProperty.builder()
								.withKey("key")
								.withValue("value")
								.build()))
						.build())
				.withSubGroups(List.of("/A", "/B"))
				.withMembers(List.of(RestGroupMembership.builder()
						.withCreationTs(new Date(1))
						.withEntityId(1)
						.withGroup("/")
						.withRemoteIdp("remoteIdp")
						.withTranslationProfile("profile")
						.build()))
				.build();
	}

}
