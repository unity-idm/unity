/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import io.imunity.rest.api.types.basic.RestI18nString;
import pl.edu.icm.unity.types.I18nString;

public class I18nStringMapper
{
	static RestI18nString map(I18nString source)
	{
		if (source == null)
			return null;

		return RestI18nString.builder()
				.withDefaultValue(source.getDefaultValue())
				.withValues(source.getMap())
				.build();
	}

	public static I18nString map(RestI18nString restI18nString)
	{
		if (restI18nString == null)
			return null;

		I18nString ret = new I18nString(restI18nString.defaultValue);
		ret.addAllValues(restI18nString.values);
		return ret;

	}
}
