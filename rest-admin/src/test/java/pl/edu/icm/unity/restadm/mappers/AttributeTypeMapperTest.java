/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestAttributeType;
import io.imunity.rest.api.types.basic.RestI18nString;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;

public class AttributeTypeMapperTest extends MapperTestBase<AttributeType, RestAttributeType>
{

	@Override
	protected AttributeType getAPIObject()
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
	protected RestAttributeType getRestObject()
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
	protected Pair<Function<AttributeType, RestAttributeType>, Function<RestAttributeType, AttributeType>> getMapper()
	{
		return Pair.of(AttributeTypeMapper::map, AttributeTypeMapper::map);
	}

}
