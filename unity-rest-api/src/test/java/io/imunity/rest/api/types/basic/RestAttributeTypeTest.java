/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Map;

public class RestAttributeTypeTest extends RestTypeBase<RestAttributeType>
{

	@Override
	protected String getJson()
	{
		return "{\"flags\":2,\"maxElements\":10,\"minElements\":2,\"selfModificable\":true,\"uniqueValues\":true,\"global\":true,\"syntaxState\":{},"
				+ "\"displayedName\":{\"DefaultValue\":\"disp\",\"Map\":{}},\"i18nDescription\":{\"DefaultValue\":\"desc\",\"Map\":{}},"
				+ "\"metadata\":{\"m1\":\"v1\"},\"name\":\"name\",\"syntaxId\":\"string\"}\n";

	}

	@Override
	protected RestAttributeType getObject()
	{
		return RestAttributeType.builder()
				.withName("name")
				.withSyntaxId("string")
				.withSyntaxState(MAPPER.createObjectNode())
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

}
