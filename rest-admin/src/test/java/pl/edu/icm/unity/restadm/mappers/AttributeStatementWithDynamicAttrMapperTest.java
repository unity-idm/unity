/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestAttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeStatement.ConflictResolution;

public class AttributeStatementWithDynamicAttrMapperTest
		extends MapperWithMinimalTestBase<AttributeStatement, RestAttributeStatement>
{

	@Override
	protected AttributeStatement getFullAPIObject()
	{
		return new AttributeStatement("true", "/extra", ConflictResolution.merge, "dyn", "dynExp");
	}

	@Override
	protected RestAttributeStatement getFullRestObject()
	{
		return RestAttributeStatement.builder()
				.withCondition("true")
				.withResolution("merge")
				.withExtraGroupName("/extra")
				.withDynamicAttributeName("dyn")
				.withDynamicAttributeExpression("dynExp")
				.build();
	}

	@Override
	protected AttributeStatement getMinAPIObject()
	{

		return new AttributeStatement("true", null, ConflictResolution.merge, "dyn", "dynExp");
	}

	@Override
	protected RestAttributeStatement getMinRestObject()
	{

		return RestAttributeStatement.builder()
				.withCondition("true")
				.withResolution("merge")
				.withDynamicAttributeName("dyn")
				.withDynamicAttributeExpression("dynExp")
				.build();
	}

	@Override
	protected Pair<Function<AttributeStatement, RestAttributeStatement>, Function<RestAttributeStatement, AttributeStatement>> getMapper()
	{
		return Pair.of(AttributeStatementMapper::map, AttributeStatementMapper::map);
	}
}
