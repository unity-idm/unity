/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

public class RestAttributeStatementWithDynAttrTest extends RestTypeBase<RestAttributeStatement>
{

	@Override
	protected String getJson()
	{
		return "{\"resolution\":\"merge\",\"condition\":\"true\",\"extraGroupName\":\"/A\",\"dynamicAttributeExpression\":\"exp\",\"dynamicAttributeName\":"
				+ "\"string\"}";
	}

	@Override
	protected RestAttributeStatement getObject()
	{
		return RestAttributeStatement.builder()
				.withCondition("true")
				.withDynamicAttributeExpression("exp")
				.withDynamicAttributeName("string")
				.withExtraGroupName("/A")
				.withResolution("merge")
				.build();
	}

}
