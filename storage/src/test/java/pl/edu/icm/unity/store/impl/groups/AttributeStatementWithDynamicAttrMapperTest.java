/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.function.Function;


import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;

public class AttributeStatementWithDynamicAttrMapperTest
		extends MapperWithMinimalTestBase<AttributeStatement, DBAttributeStatement>
{

	@Override
	protected AttributeStatement getFullAPIObject()
	{
		return new AttributeStatement("true", "/extra", ConflictResolution.merge, "dyn", "dynExp");
	}

	@Override
	protected DBAttributeStatement getFullDBObject()
	{
		return DBAttributeStatement.builder()
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
	protected DBAttributeStatement getMinDBObject()
	{

		return DBAttributeStatement.builder()
				.withCondition("true")
				.withResolution("merge")
				.withDynamicAttributeName("dyn")
				.withDynamicAttributeExpression("dynExp")
				.build();
	}

	@Override
	protected Pair<Function<AttributeStatement, DBAttributeStatement>, Function<DBAttributeStatement, AttributeStatement>> getMapper()
	{
		return Pair.of(AttributeStatementMapper::map, AttributeStatementMapper::map);
	}
}
