/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBAttributeStatementWithDynAttrTest extends DBTypeTestBase<DBAttributeStatement>
{

	@Override
	protected String getJson()
	{
		return "{\"resolution\":\"merge\",\"condition\":\"true\",\"extraGroupName\":\"/A\",\"dynamicAttributeExpression\":\"exp\",\"dynamicAttributeName\":"
				+ "\"string\"}";
	}

	@Override
	protected DBAttributeStatement getObject()
	{
		return DBAttributeStatement.builder()
				.withCondition("true")
				.withDynamicAttributeExpression("exp")
				.withDynamicAttributeName("string")
				.withExtraGroupName("/A")
				.withResolution("merge")
				.build();
	}

}
