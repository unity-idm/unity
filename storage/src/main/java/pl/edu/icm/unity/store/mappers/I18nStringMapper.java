/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.mappers;

import pl.edu.icm.unity.store.types.DBI18nString;
import pl.edu.icm.unity.types.I18nString;

public class I18nStringMapper
{
	public static DBI18nString map(I18nString source)
	{
		return DBI18nString.builder()
				.withDefaultValue(source.getDefaultValue())
				.withValues(source.getMap())
				.build();
	}

	public static I18nString map(DBI18nString dbI18nString)
	{
		I18nString ret = new I18nString(dbI18nString.defaultValue);
		ret.addAllValues(dbI18nString.values);
		return ret;

	}
}
