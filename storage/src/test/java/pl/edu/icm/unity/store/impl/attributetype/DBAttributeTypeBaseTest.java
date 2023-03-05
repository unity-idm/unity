/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attributetype;

import java.util.Map;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.DBI18nString;

public class DBAttributeTypeBaseTest extends DBTypeTestBase<DBAttributeTypeBase>
{

	@Override
	protected String getJson()
	{
		return "{\"flags\":2,\"maxElements\":10,\"minElements\":2,\"selfModificable\":true,\"uniqueValues\":true,\"global\":true,\"syntaxState\":{},"
				+ "\"displayedName\":{\"DefaultValue\":\"disp\",\"Map\":{}},\"i18nDescription\":{\"DefaultValue\":\"desc\",\"Map\":{}},"
				+ "\"metadata\":{\"m1\":\"v1\"}}\n";

	}

	@Override
	protected DBAttributeTypeBase getObject()
	{
		return DBAttributeTypeBase.builder()
				.withSyntaxState(MAPPER.createObjectNode())
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

}
