/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.impl.attribute.DBAttribute;

public class DBAttributeStatementWithFixedAttrTest extends DBTypeTestBase<DBAttributeStatement>
{

	@Override
	protected String getJson()
	{
		return "{\"resolution\":\"merge\",\"condition\":\"true\",\"extraGroupName\":\"/A\",\"fixedAttribute\":{\"remoteIdp\":\"remIdP\","
				+ "\"translationProfile\":\"profile\",\"values\":[\"v1\",\"v2\"],\"name\":\"attr\",\"groupPath\":\"/\",\"valueSyntax\":\"string\"}}";
	}

	@Override
	protected DBAttributeStatement getObject()
	{
		return DBAttributeStatement.builder()
				.withCondition("true")
				.withFixedAttribute(DBAttribute.builder()
						.withValueSyntax("string")
						.withName("attr")
						.withGroupPath("/")
						.withRemoteIdp("remIdP")
						.withTranslationProfile("profile")
						.withValues(List.of("v1", "v2"))
						.build())
				.withExtraGroupName("/A")
				.withResolution("merge")
				.build();
	}

}
