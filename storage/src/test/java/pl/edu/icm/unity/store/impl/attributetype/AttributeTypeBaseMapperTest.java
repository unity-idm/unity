/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attributetype;

import java.util.Map;
import java.util.function.Function;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBI18nString;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;

public class AttributeTypeBaseMapperTest extends MapperTestBase<AttributeType, DBAttributeTypeBase>
{

	@Override
	protected AttributeType getFullAPIObject()
	{
		AttributeType attributeType = new AttributeType("name", "string");
		attributeType.setDescription(new I18nString("desc"));
		attributeType.setDisplayedName(new I18nString("disp"));
		attributeType.setFlags(2);
		attributeType.setMaxElements(10);
		attributeType.setGlobal(true);
		attributeType.setMinElements(2);
		attributeType.setSelfModificable(true);
		attributeType.setValueSyntaxConfiguration(Constants.MAPPER.createObjectNode());
		attributeType.setMetadata(Map.of("m1", "v1"));
		attributeType.setUniqueValues(true);
		return attributeType;
	}

	@Override
	protected DBAttributeTypeBase getFullDBObject()
	{
		return DBAttributeTypeBase.builder()
				.withSyntaxState(Constants.MAPPER.createObjectNode())
				.withFlags(2)
				.withMaxElements(10)
				.withMinElements(2)
				.withSelfModificable(true)
				.withUniqueValues(true)
				.withGlobal(true)
				.withDisplayedName(DBI18nString.builder()
						.withDefaultValue("disp")
						.build())
				.withI18nDescription(DBI18nString.builder()
						.withDefaultValue("desc")
						.build())
				.withMetadata(Map.of("m1", "v1"))
				.build();
	}

	@Override
	protected Pair<Function<AttributeType, DBAttributeTypeBase>, Function<DBAttributeTypeBase, AttributeType>> getMapper()
	{
		return Pair.of(AttributeTypeBaseMapper::map, a -> AttributeTypeBaseMapper.map(a, "name", "string"));
	}
}
