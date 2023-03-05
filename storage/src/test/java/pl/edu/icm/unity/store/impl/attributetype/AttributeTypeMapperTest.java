/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attributetype;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.DBI18nString;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;

public class AttributeTypeMapperTest extends MapperWithMinimalTestBase<AttributeType, DBAttributeType>
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
	protected DBAttributeType getFullDBObject()
	{
		return DBAttributeType.builder()
				.withName("name")
				.withSyntaxId("string")
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
	protected AttributeType getMinAPIObject()
	{

		AttributeType type = new AttributeType("name", "string");
		type.setDescription(new I18nString());
		return type;
	}

	@Override
	protected DBAttributeType getMinDBObject()
	{
		return DBAttributeType.builder()
				.withName("name")
				.withSyntaxId("string")
				.withMaxElements(1)
				.build();
	}

	@Test
	public void shouldSupportStringDescription()
	{
		DBAttributeType type = DBAttributeType.builder()
				.withName("name")
				.withSyntaxId("string")
				.withDescription("desc")
				.withMaxElements(1)
				.build();
		AttributeType map = AttributeTypeMapper.map(type);
		assertThat(map.getDescription()
				.getDefaultValue()).isEqualTo("desc");
	}

	@Override
	protected Pair<Function<AttributeType, DBAttributeType>, Function<DBAttributeType, AttributeType>> getMapper()
	{
		return Pair.of(AttributeTypeMapper::map, AttributeTypeMapper::map);
	}
}
