/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import io.imunity.rest.api.types.basic.RestAttributeType;
import io.imunity.rest.api.types.basic.RestI18nString;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.i18n.I18nString;

public class AttributeTypeMapperTest extends MapperWithMinimalTestBase<AttributeType, RestAttributeType>
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
	protected RestAttributeType getFullRestObject()
	{
		return RestAttributeType.builder()
				.withName("name")
				.withSyntaxId("string")
				.withSyntaxState(Constants.MAPPER.createObjectNode())
				.withFlags(2)
				.withMaxElements(10)
				.withMinElements(2)
				.withSelfModificable(true)
				.withUniqueValues(true)
				.withGlobal(true)
				.withDisplayedName(RestI18nString.builder()
						.withDefaultValue("disp")
						.build())
				.withI18nDescription(RestI18nString.builder()
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
	protected RestAttributeType getMinRestObject()
	{
		return RestAttributeType.builder()
				.withName("name")
				.withSyntaxId("string")
				.withMaxElements(1)
				.build();
	}

	@Test
	public void shouldSupportStringDescription()
	{
		RestAttributeType type = RestAttributeType.builder()
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
	protected Pair<Function<AttributeType, RestAttributeType>, Function<RestAttributeType, AttributeType>> getMapper()
	{
		return Pair.of(AttributeTypeMapper::map, AttributeTypeMapper::map);
	}
}
