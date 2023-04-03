/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.impl.attribute.DBAttribute;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class DBGroupTest extends DBTypeTestBase<DBGroupBase>
{

	@Override
	protected String getJson()
	{
		return "{\"i18nDescription\":{\"DefaultValue\":\"desc\",\"Map\":{}},\"displayedName\":{\"DefaultValue\":\"groupDisp\",\"Map\":{}},"
				+ "\"attributeStatements\":[{\"resolution\":\"merge\",\"condition\":\"cond\",\"extraGroupName\":\"/extra\","
				+ "\"fixedAttribute\":{\"values\":[\"v1\"],\"name\":\"attr\",\"groupPath\":\"/\",\"valueSyntax\":\"string\"}}],"
				+ "\"attributesClasses\":[\"attrClass2\",\"attrClass1\"],\"delegationConfiguration\":{\"enabled\":false,\"enableSubprojects\":false,"
				+ "\"logoUrl\":null,\"registrationForm\":null,\"signupEnquiryForm\":null,\"membershipUpdateEnquiryForm\":null,\"attributes\":null},"
				+ "\"properties\":[{\"key\":\"key\",\"value\":\"value\"}],\"publicGroup\":false}\n";
	}

	@Override
	protected DBGroupBase getObject()
	{
		return DBGroupBase.builder()
				.withAttributesClasses(Set.of("attrClass1", "attrClass2"))
				.withAttributeStatements(new DBAttributeStatement[]
				{ DBAttributeStatement.builder()
						.withCondition("cond")
						.withResolution("merge")
						.withExtraGroupName("/extra")
						.withFixedAttribute(DBAttribute.builder()
								.withName("attr")
								.withGroupPath("/")
								.withValueSyntax("string")
								.withValues(List.of("v1"))
								.build())
						.build() })
				.withDisplayedName(DBI18nString.builder()
						.withDefaultValue("groupDisp")
						.build())
				.withI18nDescription(DBI18nString.builder()
						.withDefaultValue("desc")
						.build())
				.withPublicGroup(false)
				.withDelegationConfiguration(DBGroupDelegationConfiguration.builder()
						.withEnabled(false)
						.withEnableSubprojects(false)
						.build())
				.withProperties(List.of(DBGroupProperty.builder()
						.withKey("key")
						.withValue("value")
						.build()))
				.build();

	}

}
