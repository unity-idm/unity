/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestAttributeStatement;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;

public class AttributeStatementWithFixedAttrMapperTest
		extends MapperWithMinimalTestBase<AttributeStatement, RestAttributeStatement>
{

	@Override
	protected AttributeStatement getFullAPIObject()
	{
		return new AttributeStatement("true", "/extra", ConflictResolution.merge,
				new Attribute("name", "string", "/", List.of("val")));
	}

	@Override
	protected RestAttributeStatement getFullRestObject()
	{
		return RestAttributeStatement.builder()
				.withCondition("true")
				.withResolution("merge")
				.withExtraGroupName("/extra")
				.withFixedAttribute(RestAttribute.builder()
						.withName("name")
						.withGroupPath("/")
						.withValueSyntax("string")
						.withValues(List.of("val"))
						.build())
				.build();
	}

	@Override
	protected AttributeStatement getMinAPIObject()
	{
		return new AttributeStatement("true", null, ConflictResolution.merge,
				new Attribute("name", "string", "/", List.of("val")));
	}

	@Override
	protected RestAttributeStatement getMinRestObject()
	{

		return RestAttributeStatement.builder()
				.withCondition("true")
				.withResolution("merge")
				.withFixedAttribute(RestAttribute.builder()
						.withName("name")
						.withGroupPath("/")
						.withValueSyntax("string")
						.withValues(List.of("val"))
						.build())
				.build();
	}

	@Override
	protected Pair<Function<AttributeStatement, RestAttributeStatement>, Function<RestAttributeStatement, AttributeStatement>> getMapper()
	{
		return Pair.of(AttributeStatementMapper::map, AttributeStatementMapper::map);
	}

}
