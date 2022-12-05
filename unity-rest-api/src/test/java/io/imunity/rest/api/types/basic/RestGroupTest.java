/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.List;
import java.util.Set;

public class RestGroupTest extends RestTypeBase<RestGroup>
{

	@Override
	protected String getJson()
	{
		return "{\"i18nDescription\":{\"DefaultValue\":\"desc\",\"Map\":{}},\"displayedName\":{\"DefaultValue\":\"groupDisp\",\"Map\":{}},\"attributeStatements\":[{\"resolution\":\"merge\",\"condition\":\"cond\",\"extraGroupName\":\"/extra\",\"fixedAttribute\":{\"values\":[\"v1\"],\"name\":\"attr\",\"groupPath\":\"/\",\"valueSyntax\":\"string\"}}],\"attributesClasses\":[\"attrClass2\",\"attrClass1\"],\"delegationConfiguration\":{\"enabled\":false,\"enableSubprojects\":false,\"logoUrl\":null,\"registrationForm\":null,\"signupEnquiryForm\":null,\"membershipUpdateEnquiryForm\":null,\"attributes\":null},\"properties\":[{\"key\":\"key\",\"value\":\"value\"}],\"publicGroup\":false,\"path\":\"/A/B/C\"}\n";
	}

	@Override
	protected RestGroup getObject()
	{
		return RestGroup.builder()
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
				.build();

	}
}
