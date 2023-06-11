/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.types;

import java.util.Map;
import java.util.function.Function;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBI18nString;
import pl.edu.icm.unity.store.types.common.I18nStringMapper;

public class I18nStringMapperTest extends MapperTestBase<I18nString, DBI18nString>
{

	@Override
	protected I18nString getFullAPIObject()
	{
		I18nString s = new I18nString("default");
		s.addValue("pl", "plVal");
		s.addValue("en", "enVal");
		return s;
	}

	@Override
	protected DBI18nString getFullDBObject()
	{
		return DBI18nString.builder()
				.withDefaultValue("default")
				.withValues(Map.of("pl", "plVal", "en", "enVal"))
				.build();
	}

	@Override
	protected Pair<Function<I18nString, DBI18nString>, Function<DBI18nString, I18nString>> getMapper()
	{
		return Pair.of(I18nStringMapper::map, I18nStringMapper::map);
	}

}
