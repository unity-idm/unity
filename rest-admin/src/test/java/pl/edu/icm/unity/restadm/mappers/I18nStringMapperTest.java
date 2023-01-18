/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestI18nString;
import pl.edu.icm.unity.types.I18nString;

public class I18nStringMapperTest extends MapperTestBase<I18nString, RestI18nString>
{

	@Override
	protected I18nString getAPIObject()
	{
		I18nString s = new I18nString("default");
		s.addValue("pl", "plVal");
		s.addValue("en", "enVal");
		return s;
	}

	@Override
	protected RestI18nString getRestObject()
	{
		return RestI18nString.builder()
				.withDefaultValue("default")
				.withValues(Map.of("pl", "plVal", "en", "enVal"))
				.build();
	}

	@Override
	protected Pair<Function<I18nString, RestI18nString>, Function<RestI18nString, I18nString>> getMapper()
	{
		return Pair.of(I18nStringMapper::map, I18nStringMapper::map);
	}

}
